package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.dynamodb.DynamoDbRequestParser;
import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes DynamoDB commands captured from the SUT and computes database heuristics for them.
 */
public class DynamoDbHandler {

    private final List<DynamoDbCommand> commands = new ArrayList<>();
    private final List<DynamoDbCommandWithDistance> evaluatedCommands = new ArrayList<>();
    private final DynamoDbRequestParser requestParser = new DynamoDbRequestParser();
    private final DynamoDbHeuristicsCalculator calculator =
            new DynamoDbHeuristicsCalculator(new TaintHandlerExecutionTracer());
    private final DynamoDbTableDataAccessor tableDataAccessor = new DynamoDbTableDataAccessor();

    private volatile boolean calculateHeuristics;
    private Object dynamoDbClient;

    /**
     * Creates a handler with heuristic calculation enabled.
     */
    public DynamoDbHandler() {
        calculateHeuristics = true;
    }

    /**
     * Clears data collected for the current action.
     */
    public void reset() {
        commands.clear();
        evaluatedCommands.clear();
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
        if (!calculateHeuristics) {
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
                evaluatedCommands.add(new DynamoDbCommandWithDistance(command, tableName,
                        new DynamoDbDistanceWithMetrics(distance, items.size(), false)));
            } catch (RuntimeException e) {
                registerFailure(command, tableName, e);
            }
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
