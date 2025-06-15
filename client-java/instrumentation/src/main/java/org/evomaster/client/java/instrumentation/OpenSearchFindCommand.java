package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

public class OpenSearchFindCommand implements Serializable {
    /**
     * Name of the index that the operation was applied to
     */
    private final String indexName;

    /**
     * Type of the documents of the index
     */
    private final String documentsType;

    /**
     * Executed FIND query
     */
    private final Object query;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public OpenSearchFindCommand(String indexName, String documentsType, Object query, boolean successfullyExecuted, long executionTime) {
        this.indexName = indexName;
        this.documentsType = documentsType;
        this.query = query;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public Object getQuery() {
        return query;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getDocumentsType() {
        return documentsType;
    }
}
