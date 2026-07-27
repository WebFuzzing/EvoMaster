package org.evomaster.client.java.instrumentation.cassandra;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The captured shape of a Cassandra table: its full column list, with CQL types and
 * partition-key/clustering-column roles, as read once from the driver's table metadata.
 */
public class CassandraTableMetadata implements Serializable {

    private final String keyspaceName;
    private final String tableName;
    private final List<CassandraColumnMetadata> columns;

    public CassandraTableMetadata(String keyspaceName, String tableName, List<CassandraColumnMetadata> columns) {
        this.keyspaceName = keyspaceName;
        this.tableName = tableName;
        this.columns = Collections.unmodifiableList(columns);
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public String getTableName() {
        return tableName;
    }

    public List<CassandraColumnMetadata> getColumns() {
        return columns;
    }

    /**
     * Structural equality, ignoring column order: two captures of the same table are considered
     * equal if they have the same columns with the same types and primary-key roles, regardless
     * of the (driver-internal, not CQL-meaningful) iteration order they were read in.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CassandraTableMetadata)) return false;
        CassandraTableMetadata that = (CassandraTableMetadata) o;
        return Objects.equals(keyspaceName, that.keyspaceName)
                && Objects.equals(tableName, that.tableName)
                && Objects.equals(asSet(columns), asSet(that.columns));
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyspaceName, tableName, asSet(columns));
    }

    private static Set<CassandraColumnMetadata> asSet(List<CassandraColumnMetadata> columns) {
        return new HashSet<>(columns);
    }
}