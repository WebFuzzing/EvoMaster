package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.update.Update;

import java.util.*;

/**
 * The TableAliasResolver class is designed to manage and resolve
 * table aliases in SQL statements. It maintains a stack-based alias
 * resolution context and supports various SQL operations including
 * SELECT, UPDATE, and DELETE statements.
 *
 * The class ensures correct mapping of aliases to corresponding tables
 * or derived tables within SQL query structures.
 *
 * Since it has no access to the database schema, it does not resolve
 * if a given table name is a physical table or a view.
 */
class TableAliasResolver {

    private static class TableAliasContext {
        private final TreeMap<String, SqlTableName> tableNameAliases = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final TreeMap<String, SqlDerivedTable> derivedTableAliases = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private boolean containsAlias(Alias alias) {
            Objects.requireNonNull(alias, "alias cannot be null");
            return containsAlias(alias.getName());
        }

        public boolean containsAlias(String aliasName) {
            Objects.requireNonNull(aliasName, "aliasName cannot be null");
            String lowerCaseAliasName = aliasName.toLowerCase();
            return tableNameAliases.containsKey(lowerCaseAliasName)
                    || derivedTableAliases.containsKey(lowerCaseAliasName);
        }

        private SqlTableReference getTableReference(String aliasName) {
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

        public void addAliasToTableName(Alias alias, Table target) {
            Objects.requireNonNull(alias, "alias cannot be null");
            Objects.requireNonNull(target, "target cannot be null");
            if (containsAlias(alias)) {
                throw new IllegalArgumentException("Alias already declared in the current context: " + alias.getName());
            }
            String aliasName = getAliasName(alias);
            tableNameAliases.put(aliasName, new SqlTableName(target));
        }

        public void addAliasToDerivedTable(Alias alias, Select subquery) {
            if (containsAlias(alias)) {
                throw new IllegalArgumentException("Alias already declared in the current context: " + alias.getName());
            }
            String aliasName = getAliasName(alias);
            derivedTableAliases.put(aliasName, new SqlDerivedTable(subquery));
        }
    }

    private final Deque<TableAliasContext> stackOfTableAliases = new ArrayDeque<>();

    /**
     * This method is called when entering a new alias context.
     * It processes the given SQL statement to collect table aliases.
     *
     * @param statement the SQL statement to process
     */
    public void enterTableAliasContext(Statement statement) {
        Objects.requireNonNull(statement, "statement cannot be null");

        createNewAliasContext();
        if (statement instanceof Update) {
            processUpdate((Update) statement);
        } else if (statement instanceof Delete) {
            processDelete((Delete) statement);
        } else if (statement instanceof Select) {
            processSelect((Select) statement);
        } else {
            throw new IllegalArgumentException("Unsupported SQL statement type: " + statement.getClass().getName());
        }
    }

    public TableAliasResolver() {
        super();
    }

    private void processJoins(List<Join> joins) {
        Objects.requireNonNull(joins, "joins cannot be null");

        for (Join join : joins) {
            processFromItem(join.getRightItem());
        }
    }

    private void processUpdate(Update update) {
        Objects.requireNonNull(update, "update cannot be null");

        if (update.getWithItemsList() != null) {
            processWithItemsList(update.getWithItemsList());
        }
        processFromItem(update.getTable());
        if (update.getStartJoins() != null) {
            processJoins(update.getStartJoins());
        }
        if (update.getFromItem() != null) {
            processFromItem(update.getFromItem());
        }
        if (update.getJoins() != null) {
            processJoins(update.getJoins());
        }
    }

    private void processDelete(Delete delete) {
        Objects.requireNonNull(delete, "delete cannot be null");

        if (delete.getWithItemsList() != null) {
            processWithItemsList(delete.getWithItemsList());
        }
        processFromItem(delete.getTable());
        if (delete.getJoins() != null) {
            processJoins(delete.getJoins());
        }
    }

    private void processSelect(Select select) {
        Objects.requireNonNull(select, "select cannot be null");

        if (select.getWithItemsList() != null) {
            processWithItemsList(select.getWithItemsList());
        }

        if (select instanceof PlainSelect) {
            processPlainSelect((PlainSelect) select);
        } else if (select instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            if (parenthesedSelect.getAlias() != null) {
                stackOfTableAliases.peek().addAliasToDerivedTable(parenthesedSelect.getAlias(), parenthesedSelect);
            }
            Select innerSelect = parenthesedSelect.getSelect();
            processSelect(innerSelect);
        }

    }

    private void createNewAliasContext() {
        stackOfTableAliases.push(new TableAliasContext());
    }

    private void processWithItemsList(List<WithItem> withItemsList) {
        for (WithItem withItem : withItemsList) {
            if (withItem.getAlias() != null) {
                final Select subquery = withItem.getSelect();
                stackOfTableAliases.peek().addAliasToDerivedTable(withItem.getAlias(), subquery);
            }
        }
    }

    private void processPlainSelect(PlainSelect select) {
        Objects.requireNonNull(select, "select cannot be null");

        PlainSelect plainSelect = select;
        if (plainSelect.getFromItem() != null) {
            processFromItem(plainSelect.getFromItem());
        }
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                processFromItem(join.getRightItem());
            }
        }
    }

    private void processFromItem(FromItem fromItem) {
        Objects.requireNonNull(fromItem, "fromItem cannot be null");

        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            if (table.getAlias() != null) {
                final String lowerCaseAliasName = table.getAlias().getName().toLowerCase();
                final String schemaName = table.getSchemaName();
                final String tableName = table.getName();
                if (schemaName == null && isAliasDeclaredInCurrentContext(tableName)) {
                    // if there is an alias, then we need to resolve to the actual table reference
                    // (e.g. could be an alias to a common table expression)
                    final SqlTableReference tableReference = this.resolveAlias(tableName);
                    if (tableReference instanceof SqlDerivedTable) {
                        SqlDerivedTable derivedTable = (SqlDerivedTable) tableReference;
                        stackOfTableAliases.peek().addAliasToDerivedTable(table.getAlias(), derivedTable.getSelect());
                    } else if (tableReference instanceof SqlTableName) {
                        SqlTableName tableNameRef = (SqlTableName) tableReference;
                        stackOfTableAliases.peek().addAliasToTableName(table.getAlias(), tableNameRef.getTable());
                    } else {
                        throw new IllegalArgumentException("Unexpected table reference type: " + tableReference.getClass().getName());
                    }
                } else {
                    // if no alias is declared in the current context, we can safely assume that it is a table from the schema
                    stackOfTableAliases.peek().addAliasToTableName(table.getAlias(), table);
                }
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect subSelect = (ParenthesedSelect) fromItem;
            if (subSelect.getAlias() != null) {
                final String lowerCaseAliasName = subSelect.getAlias().getName().toLowerCase();
                stackOfTableAliases.peek().addAliasToDerivedTable(subSelect.getAlias(), subSelect);
            }
        }
    }

    /**
     * Returns the TableReference for the given alias from the closest context
     * where it was defined
     *
     * @param alias the alias to resolve
     * @return a TableReference object with the table or the derived table (e.g. view)
     */
    public SqlTableReference resolveAlias(String alias) {
        if (!isAliasDeclaredInAnyContext(alias)) {
            throw new IllegalArgumentException("Alias not found in any context: " + alias);
        }

        /*
         * The Deque<> iterator traverses the stack of context maps
         * in a LIFO (Last-In-First-Out) order.
         */
        for (TableAliasContext context : stackOfTableAliases) {
            if (context.containsAlias(alias)) {
                return context.getTableReference(alias);
            }
        }
        throw new IllegalArgumentException("Alias not found in any context: " + alias);
    }

    /**
     * Removes all the aliases declared in the current alias context.
     */
    public void exitTableAliasContext() {
        stackOfTableAliases.pop();
    }

    /**
     * Returns the current depth of the alias context stack.
     *
     * @return The current depth of the alias context stack.
     */
    public int getContextDepth() {
        return stackOfTableAliases.size();
    }

    /**
     * Checks if the alias is declared in the current context.
     *
     * @param aliasName The alias to check.
     * @return true if the alias is declared in the current context, false otherwise.
     */
    public boolean isAliasDeclaredInCurrentContext(String aliasName) {
        Objects.requireNonNull(aliasName, "alias cannot be null");

        if (stackOfTableAliases.isEmpty()) {
            throw new IllegalArgumentException("Alias stack is empty. Cannot resolve alias: " + aliasName);
        }
        return stackOfTableAliases.peek().containsAlias(aliasName);
    }

    /**
     * Checks if the alias is declared in any context.
     *
     * @param aliasName
     * @return
     */
    public boolean isAliasDeclaredInAnyContext(String aliasName) {
        Objects.requireNonNull(aliasName, "alias cannot be null");
        for (TableAliasContext context : stackOfTableAliases) {
            if (context.containsAlias(aliasName)) {
                return true;
            }
        }
        return false;
    }
}
