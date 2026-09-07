package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionKey;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.evomaster.client.java.controller.dynamodb.DynamoDbRequestParser;
import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processes DynamoDB commands captured from the SUT and computes database heuristics for them.
 */
public class DynamoDbHandler {

    private final List<DynamoDbCommand> commands = new ArrayList<>();
    private final List<DynamoDbCommandWithDistance> evaluatedCommands = new ArrayList<>();
    private final List<DynamoDbFailedQuery> failedQueries = new ArrayList<>();
    private final Set<String> insertionKeys = new LinkedHashSet<>();
    private final DynamoDbRequestParser requestParser = new DynamoDbRequestParser();
    private final DynamoDbHeuristicsCalculator calculator =
            new DynamoDbHeuristicsCalculator(new TaintHandlerExecutionTracer());
    private final DynamoDbTableDataAccessor tableDataAccessor = new DynamoDbTableDataAccessor();

    private volatile boolean calculateHeuristics;
    private volatile boolean extractDynamoDbExecution;
    private Object dynamoDbClient;

    /**
     * Creates a handler with heuristic calculation enabled.
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
     * Evaluates all registered commands and consumes them.
     *
     * @return evaluated commands for the current action
     */
    public List<DynamoDbCommandWithDistance> getEvaluatedDynamoDbCommands() {
        if (!calculateHeuristics && !extractDynamoDbExecution) {
            commands.clear();
            return Collections.emptyList();
        }

        Map<String, List<Map<String, Object>>> itemsByTable = new HashMap<>();
        for (DynamoDbCommand command : commands) {
            evaluateCommand(command, itemsByTable);
        }
        commands.clear();
        return new ArrayList<>(evaluatedCommands);
    }

    /**
     * @return failed reads captured for the current action
     */
    public DynamoDbExecutionsDto getExecutionDto() {
        DynamoDbExecutionsDto dto = new DynamoDbExecutionsDto();
        dto.failedQueries = new ArrayList<>(failedQueries);
        return dto;
    }

    /**
     * Evaluates one successfully executed command and reuses table scans within the current batch.
     *
     * @param command command to evaluate
     * @param itemsByTable cached table contents
     */
    private void evaluateCommand(DynamoDbCommand command, Map<String, List<Map<String, Object>>> itemsByTable) {
        if (!command.isSuccessfullyExecuted()) {
            return;
        }

        Map<String, ParsedDynamoDbRequest> parsedByTable;
        try {
            parsedByTable = requestParser.parseByTable(command.getDdbRequest(), command.getOperationName());
        } catch (RuntimeException e) {
            registerFailures(command, command.getTableNames(), e);
            return;
        }

        for (Map.Entry<String, ParsedDynamoDbRequest> entry : parsedByTable.entrySet()) {
            ParsedDynamoDbRequest parsed = entry.getValue();
            if (parsed == null || (parsed.getKeyCondition() == null && parsed.getFilterExpression() == null)) {
                continue;
            }

            String tableName = entry.getKey();
            try {
                List<Map<String, Object>> items = itemsByTable.get(tableName);
                if (items == null) {
                    items = tableDataAccessor.getItems(dynamoDbClient, tableName);
                    itemsByTable.put(tableName, items);
                }
                double distance = calculator.computeDistance(
                        parsed.getKeyCondition(), parsed.getFilterExpression(), items);
                DynamoDbDistanceWithMetrics metrics = new DynamoDbDistanceWithMetrics(distance, items.size(), false);
                if (calculateHeuristics) {
                    evaluatedCommands.add(new DynamoDbCommandWithDistance(command, tableName, metrics));
                }
                if (extractDynamoDbExecution && distance > 0.0d) {
                    registerFailedQuery(command, tableName, parsed);
                }
            } catch (RuntimeException e) {
                registerFailure(command, tableName, e);
            }
        }
    }

    /**
     * Registers one positive-distance DynamoDB read when its conditions can define an insertion item.
     *
     * @param command intercepted DynamoDB read
     * @param tableName table read by the command
     * @param parsed parsed request conditions
     */
    private void registerFailedQuery(DynamoDbCommand command, String tableName, ParsedDynamoDbRequest parsed) {
        if (command.getOperationName() != DynamoDbOperationNames.GET_ITEM
                && command.getOperationName() != DynamoDbOperationNames.QUERY) {
            return;
        }

        Map<String, DynamoDbAttributeValueDto> attributes = new LinkedHashMap<>();
        if (!evaluateEqualitiesAsFlatAttributes(parsed.getKeyCondition(), attributes)
                || !evaluateEqualitiesAsFlatAttributes(parsed.getFilterExpression(), attributes)
                || attributes.isEmpty()) {
            return;
        }

        List<DynamoDbAttributeValueDto> insertionAttributes = new ArrayList<>(attributes.values());
        String insertionKey = DynamoDbInsertionKey.fromAttributes(tableName, insertionAttributes);
        if (insertionKeys.add(insertionKey)) {
            failedQueries.add(new DynamoDbFailedQuery(tableName, insertionAttributes));
        }
    }

    /**
     * Evaluates whether a condition can be represented as flat scalar insertion attributes.
     * <p>
     * The condition must be {@code null}, a conjunction, or a top-level equality whose value is a string,
     * number, or boolean. Equalities are iteratively flattened into {@code attributes}; the method returns
     * {@code false} for unsupported predicates, nested document paths, or conflicting values for one attribute.
     * For example, the resolved condition
     * {@code country = "Argentina" AND (fifaId = 10 AND captain = true)} produces the flat attributes
     * {@code country -> (STRING, Argentina)}, {@code fifaId -> (NUMBER, 10)}, and
     * {@code captain -> (BOOLEAN, true)}.
     * A syntactically supported condition can still fail: {@code country = "Argentina" AND country = "Brazil"}
     * returns {@code false}, because one flat insertion item cannot assign two different values to {@code country}.
     *
     * @param operation condition to evaluate
     * @param attributes inferred insertion attributes, populated when the condition is supported
     * @return {@code true} if the condition is representable as flat scalar attributes
     */
    private boolean evaluateEqualitiesAsFlatAttributes(
            QueryOperation operation,
            Map<String, DynamoDbAttributeValueDto> attributes) {
        if (operation == null) {
            return true;
        }

        List<QueryOperation> pending = new ArrayList<>();
        pending.add(operation);
        while (!pending.isEmpty()) {
            QueryOperation current = pending.remove(pending.size() - 1);
            if (current instanceof AndOperation) {
                List<QueryOperation> conditions = ((AndOperation) current).getConditions();
                for (int i = conditions.size() - 1; i >= 0; i--) {
                    pending.add(conditions.get(i));
                }
                continue;
            }
            if (!(current instanceof EqualsOperation<?>)) {
                return false;
            }

            EqualsOperation<?> equality = (EqualsOperation<?>) current;
            String name = equality.getFieldName();
            if (name == null || name.isEmpty() || name.contains(".") || name.contains("[")) {
                return false;
            }
            DynamoDbAttributeValueDto attribute = toAttribute(name, equality.getValue());
            if (attribute == null) {
                return false;
            }
            DynamoDbAttributeValueDto existing = attributes.get(name);
            if (existing != null && (existing.type != attribute.type || !existing.value.equals(attribute.value))) {
                return false;
            }
            attributes.put(name, attribute);
        }
        return true;
    }

    /**
     * Converts a supported scalar equality value to the DTO used for a DynamoDB insertion attribute.
     *
     * @param name attribute name
     * @param value equality value
     * @return the corresponding scalar attribute, or {@code null} when the value is unsupported
     */
    private DynamoDbAttributeValueDto toAttribute(String name, Object value) {
        if (value instanceof String) {
            return new DynamoDbAttributeValueDto(name, DynamoDbScalarTypeDto.STRING, (String) value);
        }
        if (value instanceof Number) {
            return new DynamoDbAttributeValueDto(name, DynamoDbScalarTypeDto.NUMBER, String.valueOf(value));
        }
        if (value instanceof Boolean) {
            return new DynamoDbAttributeValueDto(name, DynamoDbScalarTypeDto.BOOLEAN, String.valueOf(value));
        }
        return null;
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
