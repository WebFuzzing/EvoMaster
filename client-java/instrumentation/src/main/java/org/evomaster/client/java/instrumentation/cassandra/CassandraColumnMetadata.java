package org.evomaster.client.java.instrumentation.cassandra;

import java.io.Serializable;
import java.util.Objects;

/**
 * A single column of a Cassandra table, as captured from the driver's table metadata: its name,
 * CQL type, and whether it belongs to the partition key or is a clustering column.
 */
public class CassandraColumnMetadata implements Serializable {

    private final String name;
    private final String cqlType;
    private final boolean partitionKey;
    private final boolean clusteringColumn;

    public CassandraColumnMetadata(String name, String cqlType, boolean partitionKey, boolean clusteringColumn) {
        this.name = name;
        this.cqlType = cqlType;
        this.partitionKey = partitionKey;
        this.clusteringColumn = clusteringColumn;
    }

    public String getName() {
        return name;
    }

    public String getCqlType() {
        return cqlType;
    }

    public boolean isPartitionKey() {
        return partitionKey;
    }

    public boolean isClusteringColumn() {
        return clusteringColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CassandraColumnMetadata)) return false;
        CassandraColumnMetadata that = (CassandraColumnMetadata) o;
        return partitionKey == that.partitionKey
                && clusteringColumn == that.clusteringColumn
                && Objects.equals(name, that.name)
                && Objects.equals(cqlType, that.cqlType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, cqlType, partitionKey, clusteringColumn);
    }
}