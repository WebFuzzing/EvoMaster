package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.List;

public class OpenSearchCommand implements Serializable {
    /**
     * Comma-separated list of index names to search; <code>_all</code> or
     * empty string to performs operations on all indices
     */
    private final List<String> index;

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

    public OpenSearchCommand(List<String> index, String method, Object query, long executionTime) {
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

    public List<String> getIndex() {
        return index;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
