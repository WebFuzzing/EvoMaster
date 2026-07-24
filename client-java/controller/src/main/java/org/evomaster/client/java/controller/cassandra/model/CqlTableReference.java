package org.evomaster.client.java.controller.cassandra.model;

/**
 * The keyspace/table a CQL SELECT, UPDATE, or DELETE statement targets, as extracted directly
 * from the parsed CQL AST. Identifier text is kept verbatim (including surrounding double quotes,
 * if present) so that case-sensitive, quoted identifiers can be told apart from case-insensitive,
 * unquoted ones.
 */
public class CqlTableReference {

    /**
     * The keyspace qualifier, verbatim as written (quotes included if quoted). {@code null} when
     * the statement didn't qualify the table with a keyspace (the session's default keyspace applies).
     */
    private final String keyspaceName;

    /**
     * The table name, verbatim as written (quotes included if quoted).
     */
    private final String tableName;

    public CqlTableReference(String keyspaceName, String tableName) {
        this.keyspaceName = keyspaceName;
        this.tableName = tableName;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public String getTableName() {
        return tableName;
    }
}