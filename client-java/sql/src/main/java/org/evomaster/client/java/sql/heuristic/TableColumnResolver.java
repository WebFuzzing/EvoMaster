package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.internal.SqlColumnId;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.SqlTableId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves table names and columns in a SQL query.
 * The name
 */
public class TableColumnResolver {

    /**
     * A helper class to resolve table aliases
     */
    private final TableAliasResolver tableAliasResolver;

    /**
     * A stack of the statements being analyzed, the outermost is the bottom
     * of the stack.
     */
    private final Deque<Statement> contextStatementStack = new ArrayDeque<>();

    /**
     * WARNING: in general we shouldn't use mutable DTO as internal data structures.
     * But, here, what we need is very simple (just checking for names).
     */
    private final DbInfoDto schema;


    public TableColumnResolver(DbInfoDto schema) {
        Objects.requireNonNull(schema);
        this.schema = schema;
        this.tableAliasResolver = new TableAliasResolver();
    }

    /**
     * Creates a context for resolving table names and columns in
     * the given statement.
     *
     * @param statement the new current statement context
     */
    public void enterStatementeContext(Statement statement) {
        tableAliasResolver.enterTableAliasContext(statement);
        contextStatementStack.push(statement);

    }

    /**
     * Exists the current statement context for resolving
     * table and column names.
     */
    public void exitCurrentStatementContext() {
        contextStatementStack.pop();
        tableAliasResolver.exitTableAliasContext();
    }

    /**
     * Check if two names are equal.
     * Comparisons are case-insensitive.
     *
     * @param l a non-null name
     * @param r a (potentially null) name
     * @return
     */
    private boolean equalNames(String l, String r) {
        Objects.requireNonNull(l);
        return l.equalsIgnoreCase(r);
    }

    private boolean isBaseTable(String tableName) {
        Objects.requireNonNull(tableName);

        return this.schema.tables.stream()
                .filter(t -> equalNames(t.name, tableName))
                .count() > 0;
    }

    private boolean hasColumn(SqlTableId sqlTableId, SqlColumnId sqlColumnId) {
        Objects.requireNonNull(sqlTableId);

        return this.schema.tables.stream()
                .filter(t -> new SqlTableId(t.name).equals(sqlTableId))
                .flatMap(t -> t.columns.stream())
                .filter(c -> new SqlColumnId(c.name).equals(sqlColumnId))
                .count() > 0;
    }

    /**
     * Get the current statement in the context stack.
     *
     * @return
     */
    public Statement getCurrentStatement() {
        return contextStatementStack.peek();
    }

    public SqlColumnReference resolve(Column column) {
        /*
         * The Deque<> iterator traverses the stack of context statements
         * in a LIFO (Last-In-First-Out) order.
         */
        for (Statement contextStatement : contextStatementStack) {
            SqlColumnReference sqlColumnReference = resolveInContextStatement(column, contextStatement);
            if (sqlColumnReference!=null) {
                return sqlColumnReference;
            }
        }
        /*
         * Column reference was not found in any context
         */
        return null;
    }

    private SqlColumnReference resolveInContextStatement(Column column, Statement contextStatement) {

        final SqlColumnId columnId = new SqlColumnId(column.getColumnName());
        if (contextStatement == null) {
            throw new IllegalStateException("No current select context");
        }
        if (column.getTable() != null) {
            final SqlTableReference sqlTableReference = this.resolve(column.getTable());
            if (sqlTableReference != null) {
                if (sqlTableReference instanceof SqlBaseTableReference) {
                    final SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
                    if (hasColumn(sqlBaseTableReference.getTableId(), columnId)) {
                        return new SqlColumnReference(sqlBaseTableReference, column.getColumnName());
                    }
                } else if (sqlTableReference instanceof SqlDerivedTableReference) {
                    final SqlDerivedTableReference sqlDerivedTableReference = (SqlDerivedTableReference) sqlTableReference;
                    if (findBaseTableColumnReference(sqlDerivedTableReference.getSelect(), column.getColumnName()) != null) {
                        return new SqlColumnReference(sqlDerivedTableReference, column.getColumnName());
                    }
                } else {
                    throw new IllegalArgumentException("Unknown table reference type: " + sqlTableReference.getClass().getName());
                }
            }
            // column reference was not found in this context
            return null;
        } else {
            return resolveInContextStatement(column.getColumnName(), contextStatement);
        }
    }

    /**
     *
     * @param contextIndex
     * @return
     */
    private Statement getContextStatement(int contextIndex) {
        if (contextIndex >= 0) {
            Iterator<Statement> iterator = contextStatementStack.iterator();
            for (int i = contextStatementStack.size() - (contextIndex + 1); i > 0; i--) {
                iterator.next();
            }
            return iterator.next();
        } else {
            return null;
        }
    }

    private SqlColumnReference resolveInContextStatement(String columnName, Statement contextStatement) {
        Objects.requireNonNull(columnName);

        if (contextStatement instanceof Delete) {
            Delete delete = (Delete) contextStatement;
            if (delete.getTable() != null) {
                return findBaseTableColumnReference(delete.getTable(), columnName);
            } else {
                //TODO
            }

        } else if (contextStatement instanceof Update) {
            Update update = (Update) contextStatement;
            if (update.getTable() != null) {
                return findBaseTableColumnReference(update.getTable(), columnName);
            } else {
                // TODO
            }
        } else if (contextStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) contextStatement;
            if (isColumnAlias(plainSelect, columnName)) {
                columnName = getBaseColumn(plainSelect, columnName).getColumnName();
            }
            for (FromItem fromItem : SqlParserUtils.getFromAndJoinItems(plainSelect)) {
                if (findBaseTableColumnReference(fromItem, columnName) != null) {
                    return createColumnReference(fromItem, columnName);
                }
            }
        } else if (contextStatement instanceof Select) {
            Select select = (Select) contextStatement;
            if (findBaseTableColumnReference(select, columnName) != null) {
                return new SqlColumnReference(new SqlDerivedTableReference(select), columnName);
            }
        }
        // column was not found in this context statement
        return null;
    }

    private boolean isColumnAlias(PlainSelect plainSelect, String columnName) {
        Objects.requireNonNull(plainSelect);
        Objects.requireNonNull(columnName);

        for (SelectItem<?> selectItem : plainSelect.getSelectItems()) {
            if (selectItem.getExpression() instanceof Column
                    && selectItem.getAlias() != null
                    && equalNames(selectItem.getAlias().getName(), columnName)) {
                return true;
            }
        }
        return false;
    }

    private Column getBaseColumn(PlainSelect plainSelect, String aliasColumnName) {
        Objects.requireNonNull(plainSelect);
        Objects.requireNonNull(aliasColumnName);
        if (!isColumnAlias(plainSelect, aliasColumnName)) {
            throw new IllegalArgumentException("Column " + aliasColumnName + " not found in " + plainSelect.getSelectItems());
        }
        for (SelectItem<?> selectItem : plainSelect.getSelectItems()) {
            if (selectItem.getExpression() instanceof Column
                    && selectItem.getAlias() != null
                    && equalNames(selectItem.getAlias().getName(), aliasColumnName)) {
                return (Column) selectItem.getExpression();
            }
        }
        return null;
    }

    private SqlColumnReference createColumnReference(FromItem fromItem, String columnName) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            final SqlTableReference sqlTableReference = this.resolve(table);
            return new SqlColumnReference(sqlTableReference, columnName);
        } else if (fromItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromItem;
            return createColumnReference(parenthesedFromItem.getFromItem(), columnName);
        } else {
            return new SqlColumnReference(new SqlDerivedTableReference((Select) fromItem), columnName);
        }
    }

    /**
     * Resolve the table reference for a given table name.
     *
     * @param table a non-null table . It could be a reference to an alias or a base table
     * @return
     */
    public SqlTableReference resolve(Table table) {
        Objects.requireNonNull(table);
        Objects.requireNonNull(table.getName());
        final String tableName = table.getName();
        if (tableAliasResolver.isAliasDeclaredInAnyContext(tableName)) {
            return tableAliasResolver.resolveTableReference(tableName);
        } else if (isBaseTable(tableName)) {
            return new SqlBaseTableReference(tableName);
        } else {
            // table was not found in any context
            return null;
        }
    }


    private SqlColumnReference findBaseTableColumnReference(FromItem fromItem, String columnName) {
        Objects.requireNonNull(fromItem);
        Objects.requireNonNull(columnName);

        if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            Select subquery = lateralSubSelect.getSelectBody();
            SqlColumnReference sqlColumnReference = findBaseTableColumnReference(subquery, columnName);
            if (sqlColumnReference != null) {
                return sqlColumnReference;
            }
        } else if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getFullyQualifiedName();
            SqlTableReference sqlTableReference;
            if (tableAliasResolver.isAliasDeclaredInAnyContext(tableName)) {
                sqlTableReference = tableAliasResolver.resolveTableReference(tableName);
            } else if (isBaseTable(tableName)) {
                sqlTableReference = new SqlBaseTableReference(table.getFullyQualifiedName());
            } else {
                throw new IllegalArgumentException("Table " + tableName + " not found in schema");
            }
            if (sqlTableReference instanceof SqlBaseTableReference) {
                SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
                SqlColumnId columnId = new SqlColumnId(columnName);
                if (hasColumn(sqlBaseTableReference.getTableId(), columnId)) {
                    return new SqlColumnReference(sqlTableReference, columnName);
                }
            } else if (sqlTableReference instanceof SqlDerivedTableReference) {
                SqlDerivedTableReference sqlDerivedTableReference = (SqlDerivedTableReference) sqlTableReference;
                if (findBaseTableColumnReference(sqlDerivedTableReference.getSelect(), columnName) != null) {
                    return new SqlColumnReference(sqlTableReference, columnName);
                }
            } else {
                throw new IllegalArgumentException("Table " + tableName + " not found in schema");
            }
        } else if (fromItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromItem;
            SqlColumnReference sqlColumnReference = findBaseTableColumnReference(parenthesedFromItem.getFromItem(), columnName);
            if (sqlColumnReference != null) {
                return sqlColumnReference;
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            Select subquery = parenthesedSelect.getSelect();
            SqlColumnReference sqlColumnReference = findBaseTableColumnReference(subquery, columnName);
            if (sqlColumnReference != null) {
                return sqlColumnReference;
            }
        } else if (fromItem instanceof WithItem) {
            WithItem withItem = (WithItem) fromItem;
            Select subquery = withItem.getSelect();
            SqlColumnReference sqlColumnReference = findBaseTableColumnReference(subquery, columnName);
            if (sqlColumnReference != null) {
                return sqlColumnReference;
            }
        } else if (fromItem instanceof TableFunction) {
            TableFunction tableFunction = (TableFunction) fromItem;
            // Handle table function
            throw new UnsupportedOperationException("Implement handling of table functions" + tableFunction);
        } else {
            throw new IllegalArgumentException("Unsupported from item type: " + fromItem.getClass());
        }
        // column was not found
        return null;
    }

    private SqlColumnReference findBaseTableColumnReference(final List<FromItem> fromOrJoinItems, String columnName) {
        Objects.requireNonNull(fromOrJoinItems);
        Objects.requireNonNull(columnName);

        // check if the column exists in any of them
        for (FromItem fromOrJoinItem : fromOrJoinItems) {
            SqlColumnReference sqlColumnReference = findBaseTableColumnReference(fromOrJoinItem, columnName);
            if (sqlColumnReference != null) {
                return sqlColumnReference;
            }
        }
        // column was not found
        return null;
    }


    private SqlColumnReference findBaseTableColumnReference(PlainSelect plainSelect, String columnName) {
        Objects.requireNonNull(plainSelect);
        Objects.requireNonNull(columnName);

        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            if (selectItem.getExpression() instanceof AllColumns) {
                // handle *
                AllColumns allColumns = (AllColumns) selectItem.getExpression();
                // get all tables and subqueries used in FROM and JOINs
                final List<FromItem> fromOrJoinItems = SqlParserUtils.getFromAndJoinItems(plainSelect);
                // check if the column exists in any of them
                return findBaseTableColumnReference(fromOrJoinItems, columnName);
            } else if (selectItem.getExpression() instanceof AllTableColumns) {
                // handle table.*
                AllTableColumns allTableColumns = (AllTableColumns) selectItem.getExpression();
                Table table = allTableColumns.getTable();
                return findBaseTableColumnReference(table, columnName);
            } else if (selectItem.getExpression() instanceof Column) {
                // handle column and column alias (if any)
                Column selectItemColumn = (Column) selectItem.getExpression();
                Table columnTable = selectItemColumn.getTable();
                Alias alias = selectItem.getAlias();
                if (equalNames(selectItemColumn.getColumnName(), columnName) || (alias != null && equalNames(alias.getName(), columnName))) {
                    final SqlColumnReference sqlColumnReference;
                    if (columnTable != null) {
                        sqlColumnReference = findBaseTableColumnReference(columnTable, selectItemColumn.getColumnName());
                    } else {
                        sqlColumnReference = findBaseTableColumnReference(SqlParserUtils.getFromAndJoinItems(plainSelect), selectItemColumn.getColumnName());
                    }
                    if (sqlColumnReference != null) {
                        return sqlColumnReference;
                    }
                }
            } else {
                // handle other expressions
                throw new IllegalArgumentException("Unsupported expression type: " + selectItem.getExpression().getClass());
            }
        }
        // column was not found
        return null;
    }


    private SqlColumnReference findBaseTableColumnReference(Select select, String columnName) {
        this.tableAliasResolver.enterTableAliasContext(select);
        try {
            if (select instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) select;
                return findBaseTableColumnReference(plainSelect, columnName);
            } else if (select instanceof SetOperationList) {
                // Handle UNION, INTERSECT, etc.
                SetOperationList setOperationList = (SetOperationList) select.getSelectBody();
                for (Select subquery : setOperationList.getSelects()) {
                    SqlColumnReference sqlColumnReference = findBaseTableColumnReference(subquery, columnName);
                    if (sqlColumnReference != null) {
                        return sqlColumnReference;
                    }
                }
            } else if (select instanceof WithItem) {
                // Handle WITH clause
                WithItem withItem = (WithItem) select.getSelectBody();
                Select subquery = withItem.getSelect();
                SqlColumnReference sqlColumnReference = findBaseTableColumnReference(subquery, columnName);
                if (sqlColumnReference != null) {
                    return sqlColumnReference;
                }
            } else if (select instanceof ParenthesedSelect) {
                // Handle parenthesized select
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select.getSelectBody();
                Select subquery = parenthesedSelect.getSelect();
                SqlColumnReference sqlColumnReference = findBaseTableColumnReference(subquery, columnName);
                if (sqlColumnReference != null) {
                    return sqlColumnReference;
                }
            }
            // column was not found
            return null;
        } finally {
            this.tableAliasResolver.exitTableAliasContext();
        }
    }


}
