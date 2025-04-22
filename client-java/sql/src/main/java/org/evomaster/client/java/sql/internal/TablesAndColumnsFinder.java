package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.schema.Column;
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

public class TablesAndColumnsFinder extends TablesNamesFinder {

    private final ColumnReferenceResolver columnReferenceResolver;

    public TablesAndColumnsFinder(DbInfoDto schema) {
        super();
        this.columnReferenceResolver = new ColumnReferenceResolver(schema);
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        this.columnReferenceResolver.enterStatementeContext(plainSelect);
        super.visit(plainSelect);
        this.columnReferenceResolver.exitCurrentSelectContext();
    }


    @Override
    public void visit(Update update) {
        this.columnReferenceResolver.enterStatementeContext(update);
        super.visit(update);
        for (UpdateSet updateSet: update.getUpdateSets()) {
            updateSet.getColumns().accept(this);
            updateSet.getValues().accept(this);
        }
        this.columnReferenceResolver.exitCurrentSelectContext();
    }

    @Override
    public void visit(Delete delete) {
        this.columnReferenceResolver.enterStatementeContext(delete);
        super.visit(delete);
        this.columnReferenceResolver.exitCurrentSelectContext();
    }

    private Map<SqlBaseTableReference, Set<ColumnReference>> columnReferences = new LinkedHashMap<>();

    public Map<SqlBaseTableReference, Set<ColumnReference>> getColumnReferences() {
        return columnReferences;
    }


    @Override
    public void visit(Column tableColumn) {
        super.visit(tableColumn);
        if (tableColumn.getColumnName().equals("true") || tableColumn.getColumnName().equals("false")) {
            return; // Skip boolean literals
        }
        ColumnReference columnReference = this.columnReferenceResolver.resolveColumnReference(tableColumn);
        if (columnReference == null) {
            throw new IllegalStateException("Column reference could not be resolved for: " + tableColumn);
        }
        SqlTableReference sqlTableReference = columnReference.getTableReference();
        final SqlBaseTableReference sqlBaseTableReference;
        final ColumnReference baseTableColumnReference;
        if (sqlTableReference instanceof SqlBaseTableReference) {
            sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
            baseTableColumnReference = columnReference;
        } else if (sqlTableReference instanceof SqlDerivedTableReference) {
            SqlDerivedTableReference sqlDerivedTableReference = (SqlDerivedTableReference) sqlTableReference;
            baseTableColumnReference = this.columnReferenceResolver.findBaseTableColumnReference(sqlDerivedTableReference.getSelect(), tableColumn);
            sqlBaseTableReference = (SqlBaseTableReference) baseTableColumnReference.getTableReference();
        } else {
            throw new IllegalStateException("Unexpected table reference type: " + sqlTableReference.getClass().getName());
        }
        Set<ColumnReference> columnReferencesSet = columnReferences.computeIfAbsent(sqlBaseTableReference, k -> new LinkedHashSet<>());
        columnReferencesSet.add(baseTableColumnReference);
    }



}
