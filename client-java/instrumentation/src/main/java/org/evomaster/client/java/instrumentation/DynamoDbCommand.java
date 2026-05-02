package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Info related to DynamoDB command execution. We made the decision to store the whole DynamoDB request object.
 * See DynamoDbBaseApiMethodParser for more details.
 */
public class DynamoDbCommand implements Serializable {


    /**
     * Names of the tables the operation was applied to (with Batch operations, more than one table is possible)
     */
    private final List<String> tableNames;
    /**
     * Name of the operation that was executed
     */
    private final DynamoDbOperationNames operationName;
    /**
     * Actual executed operation
     */
    private final Object ddbRequest;
    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;
    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public DynamoDbCommand(List<String> tableNames, DynamoDbOperationNames operationName, Object ddbRequest, boolean successfullyExecuted, long executionTime) {
        this.tableNames = tableNames == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(tableNames));
        this.operationName = Objects.requireNonNull(operationName, "operationName cannot be null");
        this.ddbRequest = ddbRequest;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public DynamoDbOperationNames getOperationName() {
        return operationName;
    }

    public Object getDdbRequest() {
        return ddbRequest;
    }

    public boolean isSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
