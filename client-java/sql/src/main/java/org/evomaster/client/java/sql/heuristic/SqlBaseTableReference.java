package org.evomaster.client.java.sql.heuristic;

public class SqlBaseTableReference extends SqlTableReference {

    private final String name;

    public SqlBaseTableReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * This name should be the result of combining
     * the database, schema and table names.
     *
     * @return
     */
    public String getFullyQualifiedName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlBaseTableReference) {
            SqlBaseTableReference other = (SqlBaseTableReference) obj;
            return name.equals(other.name);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }
}
