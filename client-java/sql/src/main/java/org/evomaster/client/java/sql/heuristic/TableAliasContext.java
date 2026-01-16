package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;

import java.util.Objects;
import java.util.TreeMap;

/**
 * The TableAliasContext class manages table aliases within a specific SQL context.
 * It allows adding aliases for both physical table names and derived tables (subqueries),
 * and provides methods to check for the existence of aliases and retrieve their corresponding
 */
public class TableAliasContext {
    private final TreeMap<String, SqlTableName> tableNameAliases = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeMap<String, SqlDerivedTable> derivedTableAliases = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private boolean containsAlias(Alias alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        return containsAlias(alias.getName());
    }

    /**
     * Check if the given alias is declared in the current context.
     *
     * @param aliasName
     * @return
     */
    public boolean containsAlias(String aliasName) {
        Objects.requireNonNull(aliasName, "aliasName cannot be null");
        String lowerCaseAliasName = aliasName.toLowerCase();
        return tableNameAliases.containsKey(lowerCaseAliasName)
                || derivedTableAliases.containsKey(lowerCaseAliasName);
    }

    /**
     * Get the table reference corresponding to the given alias.
     *
     * @param aliasName
     * @return
     */
    public SqlTableReference getTableReference(String aliasName) {
        Objects.requireNonNull(aliasName, "aliasName cannot be null");
        String lowerCaseAliasName = aliasName.toLowerCase();
        if (tableNameAliases.containsKey(lowerCaseAliasName)) {
            return tableNameAliases.get(lowerCaseAliasName);
        } else if (derivedTableAliases.containsKey(lowerCaseAliasName)) {
            return derivedTableAliases.get(lowerCaseAliasName);
        } else {
            throw new IllegalArgumentException("Alias not found in the current context: " + aliasName);
        }
    }

    private String getAliasName(Alias alias) {
        Objects.requireNonNull(alias, "alias cannot be null");

        return alias.getName().toLowerCase();
    }

    /**
     * Add an alias for a table name.
     *
     * @param alias
     * @param target
     */
    public void addAliasToTableName(Alias alias, Table target) {
        Objects.requireNonNull(alias, "alias cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        if (containsAlias(alias)) {
            throw new IllegalArgumentException("Alias already declared in the current context: " + alias.getName());
        }
        String aliasName = getAliasName(alias);
        tableNameAliases.put(aliasName, new SqlTableName(target));
    }

    /**
     * Add an alias for a derived table (subquery).
     *
     * @param alias
     * @param subquery
     */
    public void addAliasToDerivedTable(Alias alias, Select subquery) {
        if (containsAlias(alias)) {
            throw new IllegalArgumentException("Alias already declared in the current context: " + alias.getName());
        }
        String aliasName = getAliasName(alias);
        derivedTableAliases.put(aliasName, new SqlDerivedTable(subquery));
    }
}
