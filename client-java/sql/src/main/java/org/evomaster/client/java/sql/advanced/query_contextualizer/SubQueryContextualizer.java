package org.evomaster.client.java.sql.advanced.query_contextualizer;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.evomaster.client.java.sql.advanced.driver.Schema;
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

    private Schema schema;
    private Select select;
    private SchemaContext schemaContext;
    private EvaluationContext evaluationContext;

    private SubQueryContextualizer(Schema schema, Select select, SchemaContext schemaContext, EvaluationContext evaluationContext) {
        this.schema = schema;
        this.select = select;
        this.schemaContext = schemaContext;
        this.evaluationContext = evaluationContext;
    }

    private static SubQueryContextualizer createSubQueryContextualizer(Schema schema, Select select, SchemaContext previousSchemaContext, EvaluationContext evaluationContext) {
        SchemaContext schemaContext = previousSchemaContext.copy();
        SelectQuery query = createSelectQuery(select);
        if(!query.isSetOperationList()) {
            SchemaContextItem item = createSchemaContextItem(query.getFromTables(true), schema);
            schemaContext.add(item);
        }
        return new SubQueryContextualizer(schema, select, schemaContext, evaluationContext);
    }

    public static SubQueryContextualizer createSubQueryContextualizer(Schema schema, Select select, EvaluationContext evaluationContext) {
        return createSubQueryContextualizer(schema, select, new SchemaContext(), evaluationContext);
    }

    public String contextualize() {
        ExpressionDeParser expressionDeParser = new ExpressionDeParser() {
            @Override
            public void visit(Column column) {
                if(!schemaContext.includes(createQueryColumn(column)) && !isBooleanLiteral(column.getColumnName())) {
                    Object value = evaluationContext.getValue(createQueryColumn(column));
                    if(value instanceof String) {
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
                    createSubQueryContextualizer(schema, select, schemaContext, evaluationContext);
                getBuffer().append(subQueryContextualizer.contextualize());
            }
        };
        SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, expressionDeParser.getBuffer());
        select.accept(selectDeParser);
        return expressionDeParser.getBuffer().toString();
    }
}
