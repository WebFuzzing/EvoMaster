package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Info related to Neo4J RUN command execution.
 */
public class Neo4JRunCommand implements Serializable {
    /**
     * Executed RUN query (Cypher query string)
     */
    private final String query;

    /**
     * Query parameters:
     * key -> name of the parameter, as the query refers to it without the leading {@code $}
     * value -> value bound to it, as a plain Java object (String, Long, Double, Boolean, List, Map)
     * and never one of the driver's own types, which cannot be serialized
     */
    private final Map<String, Object> parameters;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public Neo4JRunCommand(String query, Map<String, Object> parameters, boolean successfullyExecuted, long executionTime) {
        this.query = query;
        this.parameters = parameters == null ? Collections.emptyMap() : parameters;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public boolean getSuccessfullyExecuted() {
        return successfullyExecuted;
    }

}
