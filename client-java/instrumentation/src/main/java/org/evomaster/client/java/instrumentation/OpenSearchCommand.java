package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

public class OpenSearchCommand implements Serializable {
    /**
     * Name of the index that the operation was applied to
     */
    private final Object index;

    /**
     * Name of the operation that was executed
     */
    private final String method;

    /**
     * Executed operation query argument
     */
    private final Object query;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public OpenSearchCommand(Object index, String method, Object query, long executionTime) {
        this.index = index;
        this.method = method;
        this.query = query;
        this.executionTime = executionTime;
    }

    public  String getMethod() {
        return method;
    }

    public Object getQuery() {
        return query;
    }

    public Object getIndex() {
        return index;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
