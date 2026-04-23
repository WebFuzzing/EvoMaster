package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Info related to DynamoDB command execution.
 */
public class DynamoDbCommand implements Serializable {


    /**
     * Names of the tables the operation was applied to (with Batch operations, more than one table is possible)
     */
    private final List<String> tableNames;
    /**
     * Name of the operation that was executed
     */
    private final String operationName;
    /**
     * Actual executed operation
     */
    private final Object request;
    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;
    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public DynamoDbCommand(List<String> tableNames, String operationName, Object request, boolean successfullyExecuted, long executionTime) {
        this.tableNames = tableNames == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(tableNames));
        this.operationName = operationName;
        this.request = request;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public String getOperationName() {
        return operationName;
    }

    public Object getRequest() {
        return request;
    }

    public boolean isSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
