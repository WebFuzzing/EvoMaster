package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.internal.SqlParserUtils;

import java.util.*;

public class ColumnReferenceResolver {

    private final TableReferenceResolver tableReferenceResolver = new TableReferenceResolver();
    private final Deque<Select> selectStack = new ArrayDeque<>();

    /**
     * WARNING: in general we shouldn't use mutable DTO as internal data structures.
     * But, here, what we need is very simple (just checking for names).
     */
    private final DbInfoDto schema;

    public ColumnReferenceResolver(DbInfoDto schema) {
        Objects.requireNonNull(schema);
        this.schema = schema;
    }

    public void enterSelectContext(Select select) {
        tableReferenceResolver.enterAliasContext(select);
        selectStack.push(select);
    }

    public void exitCurrentSelectContext() {
        selectStack.pop();
        tableReferenceResolver.exitAliasContext();
    }

    private boolean hasBaseTable(String tableName) {
        Objects.requireNonNull(tableName);

        return this.schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(tableName))
                .count() > 0;
    }

    private boolean hasColumn(String baseTableFullyQualifiedName, Column column) {
        Objects.requireNonNull(baseTableFullyQualifiedName);

        return this.schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(baseTableFullyQualifiedName))
                .flatMap(t -> t.columns.stream())
                .filter(c -> c.name.equalsIgnoreCase(column.getColumnName()))
                .count() > 0;
    }

    public ColumnReference resolveColumnReference(Column column) {
        final String sourceColumnName = column.getColumnName();
        final Select currentSelect = selectStack.peek();
        if (currentSelect == null) {
            throw new IllegalStateException("No current select context");
        }

        if (currentSelect instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) currentSelect;
            for (SelectItem<?> selectItem : plainSelect.getSelectItems()) {
                if (selectItem.getExpression() instanceof Column && selectItem.getAlias() != null
                        && selectItem.getAlias().getName().equalsIgnoreCase(sourceColumnName)) {
                    column = (Column) selectItem.getExpression();
                }
            }
            for (FromItem fromOrJoinItem : SqlParserUtils.getFromAndJoinItems(plainSelect)) {
                if (findBaseTableColumnReference(fromOrJoinItem, column) != null) {
                    return createColumnReference(column, fromOrJoinItem, sourceColumnName);
                }
            }
        } else {
            if (findBaseTableColumnReference(currentSelect, column) != null) {
                return new ColumnReference(new SqlDerivedTableReference(currentSelect), sourceColumnName);
            }
        }
        // column was not found
        return null;
    }

    private ColumnReference createColumnReference(Column column, FromItem fromOrJoinItem, String sourceColumnName) {
        if (fromOrJoinItem instanceof Table) {
            Table table = (Table) fromOrJoinItem;
            SqlTableReference sqlTableReference;
            if (tableReferenceResolver.isAliasDeclaredInAnyContext(table.getName())) {
                sqlTableReference = tableReferenceResolver.resolveTableReference(table.getName());
            } else if (hasBaseTable(table.getName())) {
                sqlTableReference = new SqlBaseTableReference(table.getFullyQualifiedName());
            } else {
                throw new IllegalArgumentException("Table " + table.getName() + " not found in schema");
            }
            return new ColumnReference(sqlTableReference, column.getColumnName());
        } else if (fromOrJoinItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromOrJoinItem;
            return createColumnReference(column, parenthesedFromItem.getFromItem(), sourceColumnName);
        } else {
            return new ColumnReference(new SqlDerivedTableReference((Select) fromOrJoinItem), sourceColumnName);
        }
    }


    private ColumnReference findBaseTableColumnReference(FromItem fromItem, Column column) {
        if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            Select subquery = lateralSubSelect.getSelectBody();
            ColumnReference columnReference = findBaseTableColumnReference(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getFullyQualifiedName();
            SqlTableReference sqlTableReference;
            if (tableReferenceResolver.isAliasDeclaredInAnyContext(tableName)) {
                sqlTableReference = tableReferenceResolver.resolveTableReference(tableName);
            } else if (hasBaseTable(tableName)) {
                sqlTableReference = new SqlBaseTableReference(table.getFullyQualifiedName());
            } else {
                throw new IllegalArgumentException("Table " + tableName + " not found in schema");
            }
            if (sqlTableReference instanceof SqlBaseTableReference) {
                SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
                if (hasColumn(sqlBaseTableReference.getFullyQualifiedName(), column)) {
                    return new ColumnReference(sqlTableReference, column.getColumnName());
                }
            } else if (sqlTableReference instanceof SqlDerivedTableReference) {
                SqlDerivedTableReference sqlDerivedTableReference = (SqlDerivedTableReference) sqlTableReference;
                if (findBaseTableColumnReference(sqlDerivedTableReference.getSelect(), column) != null) {
                    return new ColumnReference(sqlTableReference, column.getColumnName());
                }
            } else {
                throw new IllegalArgumentException("Table " + tableName + " not found in schema");
            }
        } else if (fromItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromItem;
            ColumnReference columnReference = findBaseTableColumnReference(parenthesedFromItem.getFromItem(), column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            Select subquery = parenthesedSelect.getSelect();
            ColumnReference columnReference = findBaseTableColumnReference(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (fromItem instanceof WithItem) {
            WithItem withItem = (WithItem) fromItem;
            Select subquery = withItem.getSelect();
            ColumnReference columnReference = findBaseTableColumnReference(subquery, column);
            if (columnReference != null) {
                return columnReference;
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

    private ColumnReference findBaseTableColumnReference(final List<FromItem> fromOrJoinItems, Column column) {
        // check if the column exists in any of them
        for (FromItem fromOrJoinItem : fromOrJoinItems) {
            ColumnReference columnReference = findBaseTableColumnReference(fromOrJoinItem, column);
            if (columnReference != null) {
                return columnReference;
            }
        }
        // column was not found
        return null;
    }

    private ColumnReference findBaseTableColumnReference(PlainSelect plainSelect, Column column) {
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            if (selectItem.getExpression() instanceof AllColumns) {
                // handle *
                AllColumns allColumns = (AllColumns) selectItem.getExpression();
                // get all tables and subqueries used in FROM and JOINs
                final List<FromItem> fromOrJoinItems = SqlParserUtils.getFromAndJoinItems(plainSelect);
                // check if the column exists in any of them
                return findBaseTableColumnReference(fromOrJoinItems, column);
            } else if (selectItem.getExpression() instanceof AllTableColumns) {
                // handle table.*
                AllTableColumns allTableColumns = (AllTableColumns) selectItem.getExpression();
                Table table = allTableColumns.getTable();
                return findBaseTableColumnReference(table, column);
            } else if (selectItem.getExpression() instanceof Column) {
                // handle column and column alias (if any)
                Column selectItemColumn = (Column) selectItem.getExpression();
                Table columnTable = selectItemColumn.getTable();
                Alias alias = selectItem.getAlias();
                if (selectItemColumn.getColumnName().equalsIgnoreCase(column.getColumnName()) || (alias != null && alias.getName().equalsIgnoreCase(column.getColumnName()))) {
                    final ColumnReference columnReference;
                    if (columnTable != null) {
                        columnReference = findBaseTableColumnReference(columnTable, selectItemColumn);
                    } else {
                        columnReference = findBaseTableColumnReference(SqlParserUtils.getFromAndJoinItems(plainSelect), selectItemColumn);
                    }
                    if (columnReference != null) {
                        return columnReference;
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

    public ColumnReference findBaseTableColumnReference(Select select, Column column) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            return findBaseTableColumnReference(plainSelect, column);
        } else if (select instanceof SetOperationList) {
            // Handle UNION, INTERSECT, etc.
            SetOperationList setOperationList = (SetOperationList) select.getSelectBody();
            for (Select subquery : setOperationList.getSelects()) {
                ColumnReference columnReference = findBaseTableColumnReference(subquery, column);
                if (columnReference != null) {
                    return columnReference;
                }
            }
        } else if (select instanceof WithItem) {
            // Handle WITH clause
            WithItem withItem = (WithItem) select.getSelectBody();
            Select subquery = withItem.getSelect();
            ColumnReference columnReference = findBaseTableColumnReference(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (select instanceof ParenthesedSelect) {
            // Handle parenthesized select
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select.getSelectBody();
            Select subquery = parenthesedSelect.getSelect();
            ColumnReference columnReference = findBaseTableColumnReference(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        }
        // column was not found
        return null;
    }


}
