package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.instrumentation.DynamoDbCommand;

import java.util.Objects;

/**
 * Associates an intercepted DynamoDB command and table with its heuristic result.
 */
public final class DynamoDbCommandWithDistance {

    private final DynamoDbCommand command;
    private final String tableName;
    private final DynamoDbDistanceWithMetrics distanceWithMetrics;

    /**
     * Creates an evaluated DynamoDB command.
     *
     * @param command intercepted command
     * @param tableName evaluated table
     * @param distanceWithMetrics heuristic result
     */
    public DynamoDbCommandWithDistance(DynamoDbCommand command, String tableName,
                                       DynamoDbDistanceWithMetrics distanceWithMetrics) {
        this.command = Objects.requireNonNull(command);
        this.tableName = Objects.requireNonNull(tableName);
        this.distanceWithMetrics = Objects.requireNonNull(distanceWithMetrics);
    }

    /**
     * @return intercepted command
     */
    public DynamoDbCommand getCommand() {
        return command;
    }

    /**
     * @return evaluated table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return heuristic result and metrics
     */
    public DynamoDbDistanceWithMetrics getDistanceWithMetrics() {
        return distanceWithMetrics;
    }

    /**
     * @return stable identifier used in the extra-heuristic DTO
     */
    public String getHeuristicId() {
        return command.getOperationName() + ":" + tableName + ":" + String.valueOf(command.getDdbRequest());
    }
}
