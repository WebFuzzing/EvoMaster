package org.evomaster.client.java.sql.advanced.query_contextualizer;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext;
import org.evomaster.client.java.sql.advanced.schema_context.SchemaContext;
import org.evomaster.client.java.sql.advanced.schema_context.SchemaContextItem;
import org.evomaster.client.java.sql.advanced.select_query.SelectQuery;

import static org.evomaster.client.java.sql.advanced.helpers.LiteralsHelper.isBooleanLiteral;
import static org.evomaster.client.java.sql.advanced.schema_context.SchemaContextItem.createSchemaContextItem;
import static org.evomaster.client.java.sql.advanced.select_query.QueryColumn.createQueryColumn;
import static org.evomaster.client.java.sql.advanced.select_query.SelectQuery.createSelectQuery;

public class SubQueryContextualizer {

    public static final String QUOTE = "'";

    private Select select;
    private SchemaContext schemaContext;
    private EvaluationContext evaluationContext;
    private SqlDriver sqlDriver;

    private SubQueryContextualizer(Select select, SchemaContext schemaContext, EvaluationContext evaluationContext, SqlDriver sqlDriver) {
        this.select = select;
        this.schemaContext = schemaContext;
        this.evaluationContext = evaluationContext;
        this.sqlDriver = sqlDriver;
    }

    private static SubQueryContextualizer createSubQueryContextualizer(Select select, SchemaContext previousSchemaContext, EvaluationContext evaluationContext, SqlDriver sqlDriver) {
        SchemaContext schemaContext = previousSchemaContext.copy();
        SelectQuery query = createSelectQuery(select);
        if(!query.isSetOperationList()) {
            SchemaContextItem item = createSchemaContextItem(query.getFromTables(), sqlDriver.getSchema());
            schemaContext.add(item);
        }
        return new SubQueryContextualizer(select, schemaContext, evaluationContext, sqlDriver);
    }

    public static SubQueryContextualizer createSubQueryContextualizer(Select select, EvaluationContext evaluationContext, SqlDriver sqlDriver) {
        return createSubQueryContextualizer(select, new SchemaContext(), evaluationContext, sqlDriver);
    }

    public String contextualize() {
        ExpressionDeParser expressionDeParser = new ExpressionDeParser() {
            @Override
            public void visit(Column column) {
                if(!schemaContext.includes(createQueryColumn(column)) && !isBooleanLiteral(column.getColumnName())) {
                    Object value = evaluationContext.getValue(createQueryColumn(column));
                    if (value instanceof String) {
                        getBuffer().append(QUOTE).append(value).append(QUOTE);
                    } else {
                        getBuffer().append(value);
                    }
                } else {
                    getBuffer().append(column.getFullyQualifiedName());
                }
            }

            @Override
            public void visit(Select select) {
                SubQueryContextualizer subQueryContextualizer =
                    createSubQueryContextualizer(select, schemaContext, evaluationContext, sqlDriver);
                getBuffer().append(subQueryContextualizer.contextualize());
            }
        };
        SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, expressionDeParser.getBuffer());
        select.accept(selectDeParser);
        return expressionDeParser.getBuffer().toString();
    }
}
