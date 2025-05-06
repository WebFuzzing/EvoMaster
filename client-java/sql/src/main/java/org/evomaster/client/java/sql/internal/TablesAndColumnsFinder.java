package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.heuristic.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TablesAndColumnsFinder extends TablesNamesFinder {

    private final DbInfoDto schema;

    private final TableColumnResolver columnReferenceResolver;

    private final Set<String> booleanConstantNames;

    private final Map<SqlBaseTableReference, Set<SqlColumnReference>> columnReferences = new LinkedHashMap<>();


    public TablesAndColumnsFinder(DbInfoDto schema, Set<String> booleanConstantNames) {
        super();
        this.columnReferenceResolver = new TableColumnResolver(schema);
        this.schema = schema;
        this.booleanConstantNames = booleanConstantNames;
        this.init(true);
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        this.columnReferenceResolver.enterStatementeContext(plainSelect);
        super.visit(plainSelect);
        this.columnReferenceResolver.exitCurrentStatementContext();
    }


    @Override
    public void visit(Update update) {
        this.columnReferenceResolver.enterStatementeContext(update);
        super.visit(update);
        for (UpdateSet updateSet : update.getUpdateSets()) {
            updateSet.getColumns().accept(this);
            updateSet.getValues().accept(this);
        }
        String tableName = update.getTable().getFullyQualifiedName();
        SqlBaseTableReference sqlBaseTableReference = new SqlBaseTableReference(tableName);
        if (!this.columnReferences.containsKey(sqlBaseTableReference)) {
            this.columnReferences.put(sqlBaseTableReference, new LinkedHashSet<>());
        }
        this.columnReferenceResolver.exitCurrentStatementContext();
    }

    @Override
    public void visit(Delete delete) {
        this.columnReferenceResolver.enterStatementeContext(delete);
        super.visit(delete);
        String tableName = delete.getTable().getFullyQualifiedName();
        SqlBaseTableReference sqlBaseTableReference = new SqlBaseTableReference(tableName);
        if (!this.columnReferences.containsKey(sqlBaseTableReference)) {
            this.columnReferences.put(sqlBaseTableReference, new LinkedHashSet<>());
        }
        this.columnReferenceResolver.exitCurrentStatementContext();
    }

    public Map<SqlBaseTableReference, Set<SqlColumnReference>> getColumnReferences() {
        return columnReferences;
    }


    @Override
    public void visit(Column tableColumn) {
        super.visit(tableColumn);
        //TODO check for booleanConstantNames
        if (tableColumn.getColumnName().equals("true") || tableColumn.getColumnName().equals("false")) {
            return; // Skip boolean literals
        }
        final SqlColumnReference sqlColumnReference = this.columnReferenceResolver.resolve(tableColumn);
        if (sqlColumnReference == null) {
            throw new IllegalStateException("Column reference could not be resolved for: " + tableColumn);
        }
        final SqlTableReference sqlTableReference = sqlColumnReference.getTableReference();
        final SqlBaseTableReference sqlBaseTableReference;
        final SqlColumnReference baseTableSqlColumnReference;
        if (sqlTableReference instanceof SqlBaseTableReference) {
            sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
            baseTableSqlColumnReference = sqlColumnReference;
        } else if (sqlTableReference instanceof SqlDerivedTableReference) {
            SqlDerivedTableReference sqlDerivedTableReference = (SqlDerivedTableReference) sqlTableReference;
            baseTableSqlColumnReference = this.columnReferenceResolver.findBaseTableColumnReference(sqlDerivedTableReference.getSelect(), tableColumn.getColumnName());
            sqlBaseTableReference = (SqlBaseTableReference) baseTableSqlColumnReference.getTableReference();
        } else {
            throw new IllegalStateException("Unexpected table reference type: " + sqlTableReference.getClass().getName());
        }
        Set<SqlColumnReference> sqlColumnReferencesSet = columnReferences.computeIfAbsent(sqlBaseTableReference, k -> new LinkedHashSet<>());
        sqlColumnReferencesSet.add(baseTableSqlColumnReference);
    }


    @Override
    public void visit(AllColumns allColumns) {
        Statement statement = columnReferenceResolver.getCurrentStatement();
        Set<SqlColumnReference> selectedColumns = this.columnReferenceResolver.getColumns((Select) statement);
        for (SqlColumnReference sqlColumnReference : selectedColumns) {
            if (sqlColumnReference.getTableReference() instanceof SqlBaseTableReference) {
                SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) sqlColumnReference.getTableReference();
                Set<SqlColumnReference> sqlColumnReferencesSet = columnReferences.computeIfAbsent(sqlBaseTableReference, k -> new LinkedHashSet<>());
                sqlColumnReferencesSet.add(sqlColumnReference);
            } else if (sqlColumnReference.getTableReference() instanceof SqlDerivedTableReference) {
                /*
                 * If the table is a derived table, the columns
                 * that are used are collected when processing the
                 * subqueries that are composed there. Therefore,
                 * we do not need to add them to the collected
                 * column references map.
                 */
            } else {
                throw new IllegalStateException("Unexpected table reference type: " + sqlColumnReference.getTableReference().getClass().getName());
            }
        }
        super.visit(allColumns);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        super.visit(allTableColumns);
        SqlTableReference sqlTableReference = columnReferenceResolver.resolve(allTableColumns.getTable());
        if (sqlTableReference instanceof SqlDerivedTableReference) {
            /*
             * If the table is a derived table, the columns
             * that are used are collected when processing the
             * subqueries that are composed there. Therefore,
             * we do not need to add them to the collected
             * column references map.
             */
        } else if (sqlTableReference instanceof SqlBaseTableReference) {
            SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
            Set<SqlColumnReference> sqlColumnReferencesSet = columnReferences.computeIfAbsent(sqlBaseTableReference, k -> new LinkedHashSet<>());

            LinkedHashSet<SqlColumnReference> v = this.schema.tables.stream()
                    .filter(t -> t.name.equalsIgnoreCase(sqlBaseTableReference.getFullyQualifiedName()))
                    .flatMap(t -> t.columns.stream())
                    .map(c -> new SqlColumnReference(sqlBaseTableReference, c.name))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            sqlColumnReferencesSet.addAll(v);
        } else {
            throw new IllegalStateException("Unexpected table reference type: " + sqlTableReference.getClass().getName());
        }
    }

}
