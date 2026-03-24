package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info related to Neo4J RUN command execution.
 */
public class Neo4JRunCommand implements Serializable {
    /**
     * Executed RUN query (Cypher query string)
     */
    private final String query;

    /**
     * Query parameters (can be Map, Value, Record, or null)
     */
    private final Object parameters;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public Neo4JRunCommand(String query, Object parameters, boolean successfullyExecuted, long executionTime) {
        this.query = query;
        this.parameters = parameters;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public String getQuery() {
        return query;
    }

    public Object getParameters() {
        return parameters;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public boolean getSuccessfullyExecuted() {
        return successfullyExecuted;
    }

}
