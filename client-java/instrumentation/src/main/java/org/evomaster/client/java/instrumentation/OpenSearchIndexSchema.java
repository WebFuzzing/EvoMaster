package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of documents in an OpenSearch index.
 */
public class OpenSearchIndexSchema implements Serializable {
    private final String indexName;
    private final String indexSchema;

    public OpenSearchIndexSchema(String indexName, String indexSchema) {
        this.indexName = indexName;
        this.indexSchema = indexSchema;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexSchema() {
        return indexSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenSearchIndexSchema that = (OpenSearchIndexSchema) o;
        return Objects.equals(indexName, that.indexName) && Objects.equals(indexSchema, that.indexSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName, indexSchema);
    }

    @Override
    public String toString() {
        return "OpenSearchIndexSchema{" +
                "indexName='" + indexName + '\'' +
                ", indexSchema='" + indexSchema + '\'' +
                '}';
    }
}
