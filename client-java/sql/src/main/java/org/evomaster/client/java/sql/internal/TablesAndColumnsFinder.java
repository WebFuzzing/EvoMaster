package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.heuristic.*;

import java.util.*;
import java.util.stream.Collectors;

public class TablesAndColumnsFinder extends TablesNamesFinder {

    private final DbInfoDto schema;

    private final TableColumnResolver tableColumnResolver;

    private final Map<SqlBaseTableReference, Set<SqlColumnReference>> columnReferences = new LinkedHashMap<>();

    private final Set<SqlBaseTableReference> baseTableReferences = new LinkedHashSet<>();

    private final Set<String> otherItemNames = new HashSet<>();

    public TablesAndColumnsFinder(DbInfoDto schema) {
        super();
        this.tableColumnResolver = new TableColumnResolver(schema);
        this.schema = schema;
        this.init(true);
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        this.tableColumnResolver.enterStatementeContext(plainSelect);
        super.visit(plainSelect);
        this.tableColumnResolver.exitCurrentStatementContext();
    }


    @Override
    public void visit(Update update) {
        this.tableColumnResolver.enterStatementeContext(update);
        super.visit(update);
        for (UpdateSet updateSet : update.getUpdateSets()) {
            updateSet.getColumns().accept(this);
            updateSet.getValues().accept(this);
        }
        this.tableColumnResolver.exitCurrentStatementContext();
    }

    @Override
    public void visit(Delete delete) {
        this.tableColumnResolver.enterStatementeContext(delete);
        super.visit(delete);
        this.tableColumnResolver.exitCurrentStatementContext();
    }

    public Set<SqlBaseTableReference> getBaseTableReferences() {
        return baseTableReferences;
    }

    public Set<SqlColumnReference> getColumnReferences(SqlBaseTableReference baseTableReference) {
        Objects.requireNonNull(baseTableReference);
        if (!this.columnReferences.containsKey(baseTableReference)) {
            throw new IllegalStateException("No column references found for table: " + baseTableReference);
        }
        return columnReferences.get(baseTableReference);
    }

    public boolean hasColumnReferences(SqlBaseTableReference baseTableReference) {
        Objects.requireNonNull(baseTableReference);
        return this.columnReferences.containsKey(baseTableReference);
    }

    @Override
    public void visit(Column tableColumn) {
        super.visit(tableColumn);
        if (BooleanLiteralsHelper.isBooleanLiteral(tableColumn.getColumnName())) {
            return; // Skip boolean literals
        }
        final SqlColumnReference columnReference = this.tableColumnResolver.resolve(tableColumn);
        if (columnReference == null) {
            return; // skip table columns that cannot be resolved (e.g., ``U'')
        }
        if (columnReference.getTableReference() instanceof SqlBaseTableReference) {
            final SqlBaseTableReference baseTableReference = (SqlBaseTableReference) columnReference.getTableReference();
            addColumnReference(baseTableReference, columnReference);
        }
    }

    private void addColumnReference(SqlBaseTableReference baseTableReference, SqlColumnReference columnReference) {
        Set<SqlColumnReference> sqlColumnReferencesSet = columnReferences.computeIfAbsent(baseTableReference, k -> new LinkedHashSet<>());
        sqlColumnReferencesSet.add(columnReference);
        baseTableReferences.add(baseTableReference);
    }


    @Override
    public void visit(AllColumns allColumns) {
        Statement statement = tableColumnResolver.getCurrentStatement();
        Set<SqlColumnReference> selectedColumns = this.findColumnReferences((Select) statement);
        for (SqlColumnReference columnReference : selectedColumns) {
            if (columnReference.getTableReference() instanceof SqlBaseTableReference) {
                SqlBaseTableReference baseTableReference = (SqlBaseTableReference) columnReference.getTableReference();
                addColumnReference(baseTableReference, columnReference);
            } else if (columnReference.getTableReference() instanceof SqlDerivedTable) {
                /*
                 * If the table is a derived table, the columns
                 * that are used are collected when processing the
                 * subqueries that are composed there. Therefore,
                 * we do not need to add them to the collected
                 * column references map.
                 */
            } else {
                throw new IllegalStateException("Unexpected table reference type: " + columnReference.getTableReference().getClass().getName());
            }
        }
        super.visit(allColumns);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        super.visit(allTableColumns);
        SqlTableReference tableReference = tableColumnResolver.resolve(allTableColumns.getTable());
        if (tableReference instanceof SqlDerivedTable) {
            /*
             * If the table is a derived table, the columns
             * that are used are collected when processing the
             * subqueries that are composed there. Therefore,
             * we do not need to add them to the collected
             * column references map.
             */
        } else if (tableReference instanceof SqlBaseTableReference) {
            SqlBaseTableReference baseTableReference = (SqlBaseTableReference) tableReference;
            String schemaName = baseTableReference.getTableId().getSchemaName();
            String tableName = baseTableReference.getTableId().getTableName();
            this.schema.tables.stream()
                    .filter(t -> new TableNameMatcher(t.id).matches(schemaName, tableName))
                    .flatMap(t -> t.columns.stream())
                    .map(c -> new SqlColumnReference(baseTableReference, c.name))
                    .forEach(c -> addColumnReference(baseTableReference, c));

        } else {
            throw new IllegalStateException("Unexpected table reference type: " + tableReference.getClass().getName());
        }
    }

    @Override
    public void visit(Table tableName) {
        super.visit(tableName);

        String tableWholeName = extractTableName(tableName);
        if (!otherItemNames.contains(tableWholeName.toLowerCase())) {
            SqlTableReference tableReference = tableColumnResolver.resolve(tableName);
            if (tableReference instanceof SqlBaseTableReference) {
                SqlBaseTableReference baseTableReference = (SqlBaseTableReference) tableReference;
                baseTableReferences.add(baseTableReference);
            }
        }
    }

    @Override
    public void visit(WithItem withItem) {
        otherItemNames.add(withItem.getAlias().getName().toLowerCase());
        super.visit(withItem);
    }

    public boolean containsColumnReferences(SqlBaseTableReference baseTableReference) {
        return columnReferences.containsKey(baseTableReference);
    }

    private Set<SqlColumnReference> findColumnReferences(SqlTableId baseTableId) {
        Objects.requireNonNull(baseTableId);
        String schemaName = baseTableId.getSchemaName();
        String tableName = baseTableId.getTableName();
        return this.schema.tables.stream()
                .filter(t -> new TableNameMatcher(t.id).matches(schemaName, tableName))
                .findFirst()
                .map(t -> t.columns.stream()
                        .map(c -> new SqlColumnReference(
                                new SqlBaseTableReference(t.id.catalog, t.id.schema, t.id.name), c.name))
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    private Set<SqlColumnReference> findColumnReferences(FromItem fromItem) {
        if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            Select subquery = lateralSubSelect.getSelect();
            return findColumnReferences(subquery);
        } else if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            SqlTableReference tableReference = this.tableColumnResolver.resolve(table);
            if (tableReference != null) {
                if (tableReference instanceof SqlBaseTableReference) {
                    SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) tableReference;
                    return findColumnReferences(sqlBaseTableReference.getTableId());
                } else if (tableReference instanceof SqlDerivedTable) {
                    SqlDerivedTable sqlDerivedTable = (SqlDerivedTable) tableReference;
                    return findColumnReferences(sqlDerivedTable.getSelect());
                } else {
                    throw new IllegalArgumentException("Cannot handle reference of class " + tableReference.getClass().getName());
                }
            } else {
                // return an empty set of column references
                // if the table could not have been resolved
                return Collections.emptySet();
            }
        } else if (fromItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem parenthesedFromItem = (ParenthesedFromItem) fromItem;
            return findColumnReferences(parenthesedFromItem.getFromItem());
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            Select subquery = parenthesedSelect.getSelect();
            return findColumnReferences(subquery);
        } else if (fromItem instanceof TableFunction) {
            TableFunction tableFunction = (TableFunction) fromItem;
            // Handle table function
            throw new UnsupportedOperationException("Implement handling of table functions" + tableFunction);
        } else {
            throw new IllegalArgumentException("Unsupported from item type: " + fromItem.getClass());
        }
    }

    /**
     * Returns column references for all columns used in the
     * SELECT statement. It does not return the columns if
     * the columns are used exclusively in the ON-clauses,
     * the WHERE-clauses or within subqueries.
     *
     * @param select
     * @return
     */
    private Set<SqlColumnReference> findColumnReferences(Select select) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            List<FromItem> fromItemList = SqlParserUtils.getFromAndJoinItems(plainSelect);
            Set<SqlColumnReference> columns = new LinkedHashSet<>();
            for (FromItem fromItem : fromItemList) {
                Set<SqlColumnReference> fromItemColumns = findColumnReferences(fromItem);
                columns.addAll(fromItemColumns);
            }
            return columns;
        } else if (select instanceof SetOperationList) {
            // Handle UNION, INTERSECT, etc.
            SetOperationList setOperationList = (SetOperationList) select;
            Set<SqlColumnReference> columns = new LinkedHashSet<>();
            for (Select subquery : setOperationList.getSelects()) {
                Set<SqlColumnReference> subqueryColumns = findColumnReferences(subquery);
                columns.addAll(subqueryColumns);
            }
            return columns;
        } else if (select instanceof WithItem) {
            // Handle WITH clause
            WithItem withItem = (WithItem) select;
            Select subquery = withItem.getSelect();
            return findColumnReferences(subquery);
        } else if (select instanceof ParenthesedSelect) {
            // Handle parenthesized select
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            Select subquery = parenthesedSelect.getSelect();
            return findColumnReferences(subquery);
        } else {
            throw new IllegalArgumentException("Unsupported select type: " + select.getClass());
        }
    }


}
