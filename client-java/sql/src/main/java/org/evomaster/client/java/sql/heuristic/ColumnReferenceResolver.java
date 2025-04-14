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

    private boolean hasColumn(Table baseTable, Column column) {
        Objects.requireNonNull(baseTable);

        return this.schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(baseTable.getFullyQualifiedName()))
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
                if (findColumn(fromOrJoinItem, column) != null) {
                    return createColumnReference(column, fromOrJoinItem, sourceColumnName);
                }
            }
        } else {
            if (findColumn(currentSelect, column) != null) {
                return new ColumnReference(TableReference.createDerivedTableReference(currentSelect), sourceColumnName);
            }
        }
        // column was not found
        return null;
    }

    private ColumnReference createColumnReference(Column column, FromItem fromOrJoinItem, String sourceColumnName) {
        if (fromOrJoinItem instanceof Table) {
            Table table = (Table) fromOrJoinItem;
            TableReference tableReference;
            if (tableReferenceResolver.isAliasDeclaredInAnyContext(table.getName())) {
                tableReference = tableReferenceResolver.resolveTableReference(table.getName());
            } else if (hasBaseTable(table.getName())) {
                tableReference = TableReference.createBaseTableReference(table);
            } else {
                throw new IllegalArgumentException("Table " + table.getName() + " not found in schema");
            }
            return new ColumnReference(tableReference, column.getColumnName());
        } else if (fromOrJoinItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromOrJoinItem;
            return createColumnReference(column, parenthesedFromItem.getFromItem(), sourceColumnName);
        } else {
            return new ColumnReference(TableReference.createDerivedTableReference((Select) fromOrJoinItem), sourceColumnName);
        }
    }


    private ColumnReference findColumn(FromItem fromItem, Column column) {
        if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            Select subquery = lateralSubSelect.getSelectBody();
            ColumnReference columnReference = findColumn(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getFullyQualifiedName();
            TableReference tableReference;
            if (tableReferenceResolver.isAliasDeclaredInAnyContext(tableName)) {
                tableReference = tableReferenceResolver.resolveTableReference(tableName);
            } else if (hasBaseTable(tableName)) {
                tableReference = TableReference.createBaseTableReference(table);
            } else {
                throw new IllegalArgumentException("Table " + tableName + " not found in schema");
            }
            if (tableReference.isBaseTableReference()) {
                if (hasColumn(tableReference.getBaseTable(), column)) {
                    return new ColumnReference(tableReference, column.getColumnName());
                }
            } else if (tableReference.isDerivedTableReference()) {
                if (findColumn(tableReference.getDerivedTableSelect(), column) != null) {
                    return new ColumnReference(tableReference, column.getColumnName());
                }
            }
        } else if (fromItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromItem;
            ColumnReference columnReference = findColumn(parenthesedFromItem.getFromItem(), column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            Select subquery = parenthesedSelect.getSelect();
            ColumnReference columnReference = findColumn(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (fromItem instanceof WithItem) {
            WithItem withItem = (WithItem) fromItem;
            Select subquery = withItem.getSelect();
            ColumnReference columnReference = findColumn(subquery, column);
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

    private ColumnReference findColumn(final List<FromItem> fromOrJoinItems, Column column) {
        // check if the column exists in any of them
        for (FromItem fromOrJoinItem : fromOrJoinItems) {
            ColumnReference columnReference = findColumn(fromOrJoinItem, column);
            if (columnReference != null) {
                return columnReference;
            }
        }
        // column was not found
        return null;
    }

    private ColumnReference findColumn(PlainSelect plainSelect, Column column) {
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            if (selectItem.getExpression() instanceof AllColumns) {
                // handle *
                AllColumns allColumns = (AllColumns) selectItem.getExpression();
                // get all tables and subqueries used in FROM and JOINs
                final List<FromItem> fromOrJoinItems = SqlParserUtils.getFromAndJoinItems(plainSelect);
                // check if the column exists in any of them
                return findColumn(fromOrJoinItems, column);
            } else if (selectItem.getExpression() instanceof AllTableColumns) {
                // handle table.*
                AllTableColumns allTableColumns = (AllTableColumns) selectItem.getExpression();
                Table table = allTableColumns.getTable();
                return findColumn(table, column);
            } else if (selectItem.getExpression() instanceof Column) {
                // handle column and column alias (if any)
                Column selectItemColumn = (Column) selectItem.getExpression();
                Table columnTable = selectItemColumn.getTable();
                Alias alias = selectItem.getAlias();
                if (selectItemColumn.getColumnName().equalsIgnoreCase(column.getColumnName()) || (alias != null && alias.getName().equalsIgnoreCase(column.getColumnName()))) {
                    final ColumnReference columnReference;
                    if (columnTable != null) {
                        columnReference = findColumn(columnTable, selectItemColumn);
                    } else {
                        columnReference = findColumn(SqlParserUtils.getFromAndJoinItems(plainSelect), selectItemColumn);
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

    private ColumnReference findColumn(Select select, Column column) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            return findColumn(plainSelect, column);
        } else if (select instanceof SetOperationList) {
            // Handle UNION, INTERSECT, etc.
            SetOperationList setOperationList = (SetOperationList) select.getSelectBody();
            for (Select subquery : setOperationList.getSelects()) {
                ColumnReference columnReference = findColumn(subquery, column);
                if (columnReference != null) {
                    return columnReference;
                }
            }
        } else if (select instanceof WithItem) {
            // Handle WITH clause
            WithItem withItem = (WithItem) select.getSelectBody();
            Select subquery = withItem.getSelect();
            ColumnReference columnReference = findColumn(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        } else if (select instanceof ParenthesedSelect) {
            // Handle parenthesized select
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select.getSelectBody();
            Select subquery = parenthesedSelect.getSelect();
            ColumnReference columnReference = findColumn(subquery, column);
            if (columnReference != null) {
                return columnReference;
            }
        }
        // column was not found
        return null;
    }


}
