package org.evomaster.client.java.controller.api.dto.database.cassandra;

import java.util.Objects;

/**
 * A single column of a Cassandra table, as read from the driver's own metadata on the SUT side and
 * reported to the core process.
 */
public class CassandraColumnDto {

    private String name;

    /**
     * The type of the column, as named in CQL, eg "text", "int", "map&lt;text, int&gt;".
     */
    private String cqlType;

    /**
     * Whether this column is part of the partition key of the table.
     */
    private boolean partitionKey;

    /**
     * Whether this column is one of the clustering columns of the table.
     */
    private boolean clusteringColumn;

    /**
     * Needed to deserialize the DTO, as it is sent over HTTP.
     */
    public CassandraColumnDto() {
    }

    /**
     * @param name             the name of the column
     * @param cqlType          the type of the column, as named in CQL
     * @param partitionKey     whether this column is part of the partition key of the table
     * @param clusteringColumn whether this column is one of the clustering columns of the table
     * @throws NullPointerException if the name or the type is null
     */
    public CassandraColumnDto(String name, String cqlType, boolean partitionKey, boolean clusteringColumn) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.cqlType = Objects.requireNonNull(cqlType, "cqlType cannot be null");
        this.partitionKey = partitionKey;
        this.clusteringColumn = clusteringColumn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public String getCqlType() {
        return cqlType;
    }

    public void setCqlType(String cqlType) {
        this.cqlType = Objects.requireNonNull(cqlType, "cqlType cannot be null");
    }

    public boolean isPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(boolean partitionKey) {
        this.partitionKey = partitionKey;
    }

    public boolean isClusteringColumn() {
        return clusteringColumn;
    }

    public void setClusteringColumn(boolean clusteringColumn) {
        this.clusteringColumn = clusteringColumn;
    }
}