package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionKeyBuilder;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto;
import org.evomaster.client.java.controller.dynamodb.ReflectionBasedDynamoDbClient;
import org.evomaster.client.java.controller.dynamodb.DynamoDbRequestParser;
import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbAttributeFormat;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Processes DynamoDB commands captured from the SUT and computes database heuristics for them.
 */
public class DynamoDbHandler {

    /** Commands captured from the SUT and awaiting evaluation. */
    private final List<DynamoDbCommand> commands = new ArrayList<>();

    /** Heuristic results accumulated for the current action. */
    private final List<DynamoDbCommandWithDistance> evaluatedCommands = new ArrayList<>();

    /** Failed reads accumulated for insertion generation during the current action. */
    private final List<DynamoDbFailedQuery> failedQueries = new ArrayList<>();

    /** Canonical keys used to avoid reporting duplicate inferred insertions. */
    private final Set<String> insertionKeys = new LinkedHashSet<>();

    /**
     * Maps each table name to its immutable base-table primary-key attributes and supported scalar types.
     * Definitions are loaded lazily through DescribeTable and retained across {@link #reset()} calls,
     * avoiding repeated schema reads when completing inferred insertions. Secondary-index keys are excluded.
     * Cleared by {@link #setDynamoDbClient(Object)} because another client can refer to different table schemas.
     */
    private final Map<String, DynamoDbPrimaryKey> primaryKeysByTable = new LinkedHashMap<>();

    /** Parser used to extract predicates and table names from captured requests. */
    private final DynamoDbRequestParser requestParser = new DynamoDbRequestParser();

    /** Maximum number of failed-query insertion candidates emitted per table evaluation. */
    public static final int MAX_CANDIDATES_PER_TABLE = 4;

    /** Calculator used to measure how close table items are to satisfying a request. */
    private final DynamoDbHeuristicsCalculator calculator =
            new DynamoDbHeuristicsCalculator(new TaintHandlerExecutionTracer());

    /** Collector used to extract best-effort scalar seeds from failed-read predicates. */
    private final DynamoDbSeedCollector seedCollector = new DynamoDbSeedCollector();

    /** Whether captured commands should produce heuristic results. */
    private volatile boolean calculateHeuristics;

    /** Whether failed reads should be captured for insertion generation. */
    private volatile boolean extractDynamoDbExecution;

    /** SDK v2 synchronous or asynchronous DynamoDB client supplied by the SUT controller. */
    private Object dynamoDbClient;

    /**
     * Creates a handler with heuristic calculation and failed-read extraction enabled.
     */
    public DynamoDbHandler() {
        calculateHeuristics = true;
        extractDynamoDbExecution = true;
    }

    /**
     * Clears data collected for the current action.
     */
    public void reset() {
        commands.clear();
        evaluatedCommands.clear();
        failedQueries.clear();
        insertionKeys.clear();
    }

    /**
     * @return whether DynamoDB heuristic calculation is enabled
     */
    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    /**
     * Enables or disables DynamoDB heuristic calculation.
     *
     * @param calculateHeuristics new calculation state
     */
    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    /**
     * @return whether failed DynamoDB reads are extracted
     */
    public boolean isExtractDynamoDbExecution() {
        return extractDynamoDbExecution;
    }

    /**
     * Enables or disables extraction of failed DynamoDB reads.
     *
     * @param extractDynamoDbExecution new extraction state
     */
    public void setExtractDynamoDbExecution(boolean extractDynamoDbExecution) {
        this.extractDynamoDbExecution = extractDynamoDbExecution;
    }

    /**
     * Sets the SDK v2 client used to read table contents.
     *
     * @param dynamoDbClient synchronous or asynchronous DynamoDB client
     */
    public void setDynamoDbClient(Object dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        //primaryKeysByTable is the only schema metadata retained across reset() calls so it needs to be cleared here.
        primaryKeysByTable.clear();
    }

    /**
     * Registers one intercepted DynamoDB command.
     *
     * @param command intercepted command
     */
    public void handle(DynamoDbCommand command) {
        if (command != null) {
            commands.add(command);
        }
    }

    /**
     * Evaluates all pending {@link #commands} registered via {@link #handle(DynamoDbCommand)}, clears the
     * pending queue, and returns the accumulated distance entries for the current action.
     * <p>
     * Results in {@link #evaluatedCommands} and {@link #failedQueries} are retained across calls until
     * {@link #reset()}, so subsequent calls in the same action return the previously evaluated snapshot.
     *
     * @return copy of all evaluated commands with heuristic distances for the current action
     */
    public List<DynamoDbCommandWithDistance> getEvaluatedDynamoDbCommands() {
        if (!calculateHeuristics && !extractDynamoDbExecution) {
            // Neither heuristic scoring nor failed-read extraction is enabled; discard queued commands
            // without scanning DynamoDB tables.
            commands.clear();
            return Collections.emptyList();
        }

        if (dynamoDbClient == null) {
            for (DynamoDbCommand command : commands) {
                registerFailures(command, command.getTableNames(),
                        new IllegalStateException("DynamoDB client is not configured"));
            }
            commands.clear();
            return new ArrayList<>(evaluatedCommands);
        }

        // Reuse table scans within this batch so each referenced table is scanned at most once.
        DynamoDbTableItemsCache itemsCache = new DynamoDbTableItemsCache(dynamoDbClient);
        for (DynamoDbCommand command : commands) {
            evaluateCommand(command, itemsCache);
        }
        commands.clear();
        return new ArrayList<>(evaluatedCommands);
    }

    /**
     * Returns failed-read insertion candidates populated during {@link #getEvaluatedDynamoDbCommands()}
     * for the current action (retained until {@link #reset()}).
     *
     * @return DTO containing failed reads captured for the current action
     */
    public DynamoDbExecutionsDto getExecutionDto() {
        DynamoDbExecutionsDto dto = new DynamoDbExecutionsDto();
        dto.failedQueries = new ArrayList<>(failedQueries);
        return dto;
    }

    /**
     * Evaluates one command and reuses table scans within the current batch.
     * Failed conditional writes are evaluated because their predicates provide the
     * distance needed to guide subsequent candidates toward a successful write.
     *
     * @param command command to evaluate
     * @param itemsCache cached table contents
     */
    private void evaluateCommand(DynamoDbCommand command, DynamoDbTableItemsCache itemsCache) {
        Map<String, ParsedDynamoDbRequest> parsedByTable = parseCommandByTable(command);
        if (parsedByTable == null || parsedByTable.isEmpty()) {
            // Either request parsing failed (already logged by registerFailures) or no tables were extracted.
            return;
        }

        for (Map.Entry<String, ParsedDynamoDbRequest> entry : parsedByTable.entrySet()) {
            evaluateTableRequest(command, entry.getKey(), entry.getValue(), itemsCache);
        }
    }

    /**
     * Parses a DynamoDB request into per-table predicates, recording failure metrics if parsing throws.
     *
     * @param command intercepted DynamoDB command
     * @return per-table parsed requests, or {@code null} when parsing fails
     */
    private Map<String, ParsedDynamoDbRequest> parseCommandByTable(DynamoDbCommand command) {
        try {
            return requestParser.parseByTable(command.getDdbRequest(), command.getOperationName());
        } catch (RuntimeException e) {
            registerFailures(command, command.getTableNames(), e);
            return null;
        }
    }

    /**
     * Evaluates one table entry of a parsed DynamoDB command for heuristic distances and failed-read extraction.
     *
     * @param command intercepted DynamoDB command
     * @param tableName table targeted by {@code parsed}
     * @param parsed parsed predicates for {@code tableName}
     * @param itemsCache cached table contents for the current batch
     */
    private void evaluateTableRequest(DynamoDbCommand command, String tableName, ParsedDynamoDbRequest parsed,
                                      DynamoDbTableItemsCache itemsCache) {
        if (!shouldEvaluateTableRequest(command, tableName, parsed)) {
            return;
        }

        try {
            List<Map<String, Object>> items = itemsCache.getItems(tableName);
            double distance = calculator.computeDistance(
                    parsed.getKeyCondition(), parsed.getFilterExpression(), items);
            DynamoDbDistanceWithMetrics metrics = new DynamoDbDistanceWithMetrics(distance, items.size(), false);
            if (calculateHeuristics) {
                evaluatedCommands.add(new DynamoDbCommandWithDistance(command, tableName, metrics));
            }
            //Batch can have distance 0.0 if it satisfies a condition for 1 item, we want to explore all conditions here
            if (extractDynamoDbExecution
                    && (distance > 0.0d || command.getOperationName() == DynamoDbOperationNames.BATCH_GET_ITEM)) {
                registerFailedQuery(command, tableName, parsed, items);
            }
        } catch (RuntimeException e) {
            registerFailure(command, tableName, e);
        }
    }

    /**
     * Returns whether a parsed table request carries predicates that should be evaluated.
     *
     * @param command intercepted DynamoDB command
     * @param tableName table targeted by {@code parsed}
     * @param parsed parsed predicates for {@code tableName}
     * @return {@code true} if distance/insertion evaluation should run for this table entry
     */
    private boolean shouldEvaluateTableRequest(DynamoDbCommand command, String tableName,
                                               ParsedDynamoDbRequest parsed) {
        if (parsed == null || (parsed.getKeyCondition() == null && parsed.getFilterExpression() == null)) {
            SimpleLogger.debug("Skipping DynamoDB evaluation for table '" + tableName
                    + "': command has no key condition or filter/condition expression");
            return false;
        }
        if (!command.isSuccessfullyExecuted() && parsed.getFilterExpression() == null) {
            SimpleLogger.debug("Skipping failed DynamoDB command evaluation for table '" + tableName
                    + "': no filter or condition expression present");
            return false;
        }
        return true;
    }

    /**
     * Registers failed-read insertion candidates when a supported DynamoDB read's predicates can seed items.
     *
     * @param command intercepted DynamoDB read
     * @param tableName table read by the command
     * @param parsed parsed request conditions
     * @param items existing table items that can supply non-key attributes
     */
    private void registerFailedQuery(DynamoDbCommand command, String tableName, ParsedDynamoDbRequest parsed,
                                     List<Map<String, Object>> items) {
        if (!isInsertionInferableOperation(command.getOperationName())) {
            return;
        }
        if (command.getOperationName() == DynamoDbOperationNames.BATCH_GET_ITEM) {
            registerBatchGetFailedQueries(command, tableName, parsed, items);
            return;
        }
        DynamoDbSeedCollector.CollectedSeeds seeds = seedCollector.collect(
                parsed.getKeyCondition(), parsed.getFilterExpression());
        if (seeds == null) {
            SimpleLogger.debug("Skipping DynamoDB failed-query extraction for table '" + tableName
                    + "': predicates yielded no flat scalar insertion seeds");
            return;
        }
        DynamoDbPrimaryKey primaryKey = getPrimaryKey(tableName);
        if (primaryKey == null) {
            return;
        }
        registerSeededCandidate(command, tableName, seeds, primaryKey, items);
    }

    /**
     * Registers insertion candidates for {@code BatchGetItem} keys not matched by any existing item,
     * capped at {@link #MAX_CANDIDATES_PER_TABLE}.
     *
     * @param command intercepted {@code BatchGetItem} command
     * @param tableName target table
     * @param parsed parsed per-table key conditions
     * @param items existing normalized table items
     */
    private void registerBatchGetFailedQueries(DynamoDbCommand command, String tableName, ParsedDynamoDbRequest parsed,
                                               List<Map<String, Object>> items) {
        List<QueryOperation> branches = seedCollector.unpackBatchGetBranches(parsed.getKeyCondition());
        DynamoDbPrimaryKey primaryKey = null;
        int emitted = 0;
        for (QueryOperation branch : branches) {
            if (emitted >= MAX_CANDIDATES_PER_TABLE) {
                break;
            }
            if (isBranchSatisfiedByAnyItem(branch, items)) {
                continue;
            }
            DynamoDbSeedCollector.CollectedSeeds seeds = seedCollector.collect(branch, null);
            if (seeds == null) {
                continue;
            }
            if (primaryKey == null) {
                primaryKey = getPrimaryKey(tableName);
                if (primaryKey == null) {
                    return;
                }
            }
            if (registerSeededCandidate(command, tableName, seeds, primaryKey, items)) {
                emitted++;
            }
        }
    }

    /** Returns whether any existing item already satisfies a single key branch. */
    private boolean isBranchSatisfiedByAnyItem(QueryOperation branch, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            if (calculator.computeExpression(branch, item).isTrue()) {
                return true;
            }
        }
        return false;
    }

    /** Validates, completes, and records one seeded insertion candidate. */
    private boolean registerSeededCandidate(DynamoDbCommand command, String tableName,
                                            DynamoDbSeedCollector.CollectedSeeds seeds,
                                            DynamoDbPrimaryKey primaryKey,
                                            List<Map<String, Object>> items) {
        Set<String> primaryKeyNames = primaryKey.getAttributes().keySet();
        if (!seedCollector.applyPrimaryKeyEmptyStringGuard(seeds, primaryKeyNames)) {
            SimpleLogger.debug("Skipping DynamoDB failed-query extraction for table '" + tableName
                    + "': exact empty-string primary key cannot be inserted");
            return false;
        }
        Map<String, DynamoDbAttributeValueDto> attributes = seeds.getAttributes();
        if (!matchesPrimaryKeyTypes(attributes, primaryKey)) {
            SimpleLogger.debug("Skipping DynamoDB failed-query extraction for table '" + tableName
                    + "': inferred attribute types do not match table primary key");
            return false;
        }
        completeFromTableItem(tableName, attributes, primaryKeyNames, items);
        Map<String, DynamoDbKeyScalarTypeDto> missingKeys = computeMissingPrimaryKeys(primaryKey, attributes);
        DynamoDbFailedQuery inferred = addFailedQueryIfAbsent(tableName, attributes, missingKeys);
        if (inferred == null) {
            return false;
        }
        populatePrimaryKeyFormats(command, inferred);
        populatePrimaryKeyValueExamples(missingKeys.keySet(), items, inferred);
        return true;
    }

    /**
     * Returns whether the DynamoDB operation can seed a concrete row from failed-read predicates.
     *
     * @param operationName DynamoDB operation name
     * @return {@code true} for {@code GetItem}, {@code Query}, {@code Scan}, and {@code BatchGetItem}
     */
    private boolean isInsertionInferableOperation(DynamoDbOperationNames operationName) {
        return operationName == DynamoDbOperationNames.GET_ITEM
                || operationName == DynamoDbOperationNames.QUERY
                || operationName == DynamoDbOperationNames.SCAN
                || operationName == DynamoDbOperationNames.BATCH_GET_ITEM;
    }

    /**
     * Computes primary-key attributes required by the table schema that are not yet present in {@code attributes}.
     *
     * @param primaryKey table primary-key schema
     * @param attributes already-inferred attributes
     * @return map of missing primary-key names to their scalar types
     */
    private Map<String, DynamoDbKeyScalarTypeDto> computeMissingPrimaryKeys(
            DynamoDbPrimaryKey primaryKey, Map<String, DynamoDbAttributeValueDto> attributes) {
        Map<String, DynamoDbKeyScalarTypeDto> missingKeys = new LinkedHashMap<>(primaryKey.getAttributes());
        missingKeys.keySet().removeAll(attributes.keySet());
        return missingKeys;
    }

    /**
     * Copies any recorded enhanced-schema attribute formats from the intercepted command onto {@code inferred}.
     *
     * @param command intercepted DynamoDB command
     * @param inferred failed-query DTO to populate
     */
    private void populatePrimaryKeyFormats(DynamoDbCommand command, DynamoDbFailedQuery inferred) {
        if (command.getDdbRequest() == null) {
            return;
        }
        Map<String, DynamoDbAttributeFormat> formats = command.getDdbRequest().attributeFormats();
        if (formats == null) {
            return;
        }
        formats.forEach((attributeName, format) ->
                inferred.primaryKeyFormats.put(attributeName, format != null ? format.getValue() : null));
    }

    /**
     * Collects deterministic sample string values from existing table items for missing primary-key attributes.
     *
     * @param missingKeyNames primary-key attributes not supplied by the failed query
     * @param items existing normalized table items
     * @param inferred failed-query DTO to populate
     */
    private void populatePrimaryKeyValueExamples(Set<String> missingKeyNames, List<Map<String, Object>> items,
                                                 DynamoDbFailedQuery inferred) {
        for (String key : missingKeyNames) {
            items.stream()
                    .map(item -> item.get(key))
                    .filter(value -> value instanceof String)
                    .map(String::valueOf)
                    .sorted()
                    .findFirst()
                    .ifPresent(value -> inferred.primaryKeyValueExamples.put(key, value));
        }
    }

    /**
     * Loads and caches the base-table key, leaving failed schema reads eligible for retry.
     *
     * @param tableName table whose primary key is needed
     * @return primary-key metadata, or {@code null} when inspection fails
     */
    private DynamoDbPrimaryKey getPrimaryKey(String tableName) {
        try {
            return primaryKeysByTable.computeIfAbsent(tableName,
                    name -> new DynamoDbPrimaryKey(
                            new ReflectionBasedDynamoDbClient(dynamoDbClient).getPrimaryKeyAttributes(name)));
        } catch (RuntimeException e) {
            SimpleLogger.uniqueWarn("Failed to inspect DynamoDB primary key for table " + tableName
                    + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates supplied key types, leaving missing values for core to generate.
     *
     * @param attributes inferred query attributes
     * @param primaryKey required key names and scalar types
     * @return whether all supplied key values match the table schema
     */
    private boolean matchesPrimaryKeyTypes(Map<String, DynamoDbAttributeValueDto> attributes,
                                           DynamoDbPrimaryKey primaryKey) {
        for (Map.Entry<String, DynamoDbKeyScalarTypeDto> key : primaryKey.getAttributes().entrySet()) {
            DynamoDbAttributeValueDto existing = attributes.get(key.getKey());
            if (existing != null && existing.type != DynamoDbValueTypeDto.fromKeyScalar(key.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Records an inferred insertion once per action, using its canonical attributes for deduplication.
     *
     * @param tableName table being populated
     * @param attributes inferred query and template attributes
     * @param missingKeys key names and types that core must generate
     * @return newly recorded failed query, or {@code null} when an identical insertion was already recorded
     */
    private DynamoDbFailedQuery addFailedQueryIfAbsent(String tableName,
                                                       Map<String, DynamoDbAttributeValueDto> attributes,
                                                       Map<String, DynamoDbKeyScalarTypeDto> missingKeys) {
        List<DynamoDbAttributeValueDto> insertionAttributes = new ArrayList<>(attributes.values());
        String insertionKey = DynamoDbInsertionKeyBuilder.fromAttributes(tableName, insertionAttributes);
        if (!insertionKeys.add(insertionKey)) {
            return null;
        }
        DynamoDbFailedQuery failedQuery = new DynamoDbFailedQuery(tableName, insertionAttributes, missingKeys);
        failedQueries.add(failedQuery);
        return failedQuery;
    }

    /**
     * Copies supported non-key attributes from the richest existing item, keeping query values intact.
     * A key-only item can satisfy DynamoDB while lacking fields required by the SUT after the read.
     * Ties use the canonical attribute key so scan order does not change the chosen template.
     *
     * @param tableName table being populated
     * @param attributes inferred query values, extended in place
     * @param primaryKeys primary-key names that must not be copied from another item
     * @param items normalized table contents
     */
    private void completeFromTableItem(String tableName, Map<String, DynamoDbAttributeValueDto> attributes,
                                       Set<String> primaryKeys, List<Map<String, Object>> items) {
        Map<String, DynamoDbAttributeValueDto> template = Collections.emptyMap();
        String templateKey = null;
        for (Map<String, Object> item : items) {
            Map<String, DynamoDbAttributeValueDto> candidate = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : new TreeMap<>(item).entrySet()) {
                if (!primaryKeys.contains(entry.getKey())) {
                    DynamoDbAttributeValueDto attribute = toTemplateAttribute(entry.getKey(), entry.getValue());
                    if (attribute != null) {
                        candidate.put(entry.getKey(), attribute);
                    }
                }
            }
            String candidateKey = DynamoDbInsertionKeyBuilder.fromAttributes(tableName,
                    new ArrayList<>(candidate.values()));
            if (candidate.size() > template.size()
                    || (candidate.size() == template.size()
                    && (templateKey == null || candidateKey.compareTo(templateKey) < 0))) {
                template = candidate;
                templateKey = candidateKey;
            }
        }
        template.forEach(attributes::putIfAbsent);
    }

    /**
     * Copies normalized observed values as typed trees, skipping unsupported binary ({@code B}) and
     * binary set ({@code BS}) payloads (as well as empty or malformed sets).
     *
     * @param name attribute name
     * @param value normalized plain Java value observed in an existing item
     * @return typed template attribute, or {@code null} when the value cannot be represented
     */
    private DynamoDbAttributeValueDto toTemplateAttribute(String name, Object value) {
        try {
            return DynamoDbDtoUtils.fromPlainValue(name, value);
        } catch (IllegalArgumentException e) {
            SimpleLogger.uniqueWarn("Skipping unsupported DynamoDB template attribute '" + name + "': "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Records a failed evaluation for every table referenced by a command.
     *
     * @param command command that could not be evaluated
     * @param tableNames tables referenced by the command
     * @param cause evaluation failure
     */
    private void registerFailures(DynamoDbCommand command, List<String> tableNames, RuntimeException cause) {
        if (tableNames == null || tableNames.isEmpty()) {
            SimpleLogger.uniqueWarn("Failed to evaluate a DynamoDB command: " + cause.getMessage());
            return;
        }
        for (String tableName : tableNames) {
            registerFailure(command, tableName, cause);
        }
    }

    /**
     * Records one failed table evaluation without interrupting the SUT controller.
     *
     * @param command command that could not be evaluated
     * @param tableName affected table
     * @param cause evaluation failure
     */
    private void registerFailure(DynamoDbCommand command, String tableName, RuntimeException cause) {
        SimpleLogger.uniqueWarn("Failed to evaluate DynamoDB command for table " + tableName + ": "
                + cause.getMessage());
        evaluatedCommands.add(new DynamoDbCommandWithDistance(command, tableName,
                new DynamoDbDistanceWithMetrics(1.0d, 0, true)));
    }
}
