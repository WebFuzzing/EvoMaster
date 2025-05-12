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

public class TablesAndColumnsFinder extends TablesNamesFinder {

    private final DbInfoDto schema;

    private final TableColumnResolver columnReferenceResolver;

    private final Map<SqlBaseTableReference, Set<SqlColumnReference>> columnReferences = new LinkedHashMap<>();

    private final Set<SqlBaseTableReference> baseTableReferences = new LinkedHashSet<>();

    private Set<String> otherItemNames = new HashSet<>();

    public TablesAndColumnsFinder(DbInfoDto schema) {
        super();
        this.columnReferenceResolver = new TableColumnResolver(schema);
        this.schema = schema;
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
        this.columnReferenceResolver.exitCurrentStatementContext();
    }

    @Override
    public void visit(Delete delete) {
        this.columnReferenceResolver.enterStatementeContext(delete);
        super.visit(delete);
        this.columnReferenceResolver.exitCurrentStatementContext();
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

    @Override
    public void visit(Column tableColumn) {
        super.visit(tableColumn);
        if (BooleanLiteralsHelper.isBooleanLiteral(tableColumn.getColumnName())) {
            return; // Skip boolean literals
        }
        final SqlColumnReference columnReference = this.columnReferenceResolver.resolveToBaseTableColumnReference(tableColumn);
        if (columnReference == null) {
            throw new IllegalStateException("Column reference could not be resolved for: " + tableColumn);
        }
        final SqlBaseTableReference baseTableReference = (SqlBaseTableReference) columnReference.getTableReference();
        addColumnReference(baseTableReference, columnReference);
    }

    private void addColumnReference(SqlBaseTableReference baseTableReference, SqlColumnReference columnReference) {
        Set<SqlColumnReference> sqlColumnReferencesSet = columnReferences.computeIfAbsent(baseTableReference, k -> new LinkedHashSet<>());
        sqlColumnReferencesSet.add(columnReference);
        baseTableReferences.add(baseTableReference);
    }


    @Override
    public void visit(AllColumns allColumns) {
        Statement statement = columnReferenceResolver.getCurrentStatement();
        Set<SqlColumnReference> selectedColumns = this.columnReferenceResolver.getColumns((Select) statement);
        for (SqlColumnReference columnReference : selectedColumns) {
            if (columnReference.getTableReference() instanceof SqlBaseTableReference) {
                SqlBaseTableReference baseTableReference = (SqlBaseTableReference) columnReference.getTableReference();
                addColumnReference(baseTableReference, columnReference);
            } else if (columnReference.getTableReference() instanceof SqlDerivedTableReference) {
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
        SqlTableReference tableReference = columnReferenceResolver.resolve(allTableColumns.getTable());
        if (tableReference instanceof SqlDerivedTableReference) {
            /*
             * If the table is a derived table, the columns
             * that are used are collected when processing the
             * subqueries that are composed there. Therefore,
             * we do not need to add them to the collected
             * column references map.
             */
        } else if (tableReference instanceof SqlBaseTableReference) {
            SqlBaseTableReference baseTableReference = (SqlBaseTableReference) tableReference;

            this.schema.tables.stream()
                    .filter(t -> new SqlTableId(t.name).equals(baseTableReference.getTableId()))
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
            SqlTableReference tableReference = columnReferenceResolver.resolve(tableName);
            if (tableReference instanceof SqlBaseTableReference) {
                SqlBaseTableReference baseTableReference = (SqlBaseTableReference)tableReference;
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
}
