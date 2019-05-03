package org.evomaster.client.java.controller.internal.db.constraint.extract;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.db.constraint.TableConstraint;
import org.evomaster.client.java.controller.internal.db.constraint.UnsupportedTableConstraint;
import org.evomaster.client.java.controller.internal.db.constraint.expr.SqlCondition;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParser;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParserException;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParserFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public abstract class ConstraintExtractor {


    /**
     * Parsers a conditional expression and adds those constraints to the TableDto
     *
     * @param tableDto
     * @param condExpression
     * @throws SqlConditionParserException if the parsing of the conditional expression fails
     */
    protected List<TableConstraint> translateToConstraints(TableDto tableDto, String condExpression) {

        SqlConditionParser sqlParser = SqlConditionParserFactory.buildParser();
        String tableName = tableDto.name;
        try {
            SqlCondition expr = sqlParser.parse(condExpression);
            TranslationContext translationContext = new TranslationContext(tableName);
            SqlConditionTranslator exprExtractor = new SqlConditionTranslator(translationContext);
            List<TableConstraint> constraints = expr.accept(exprExtractor, null);
            return constraints;
        } catch (SqlConditionParserException e) {
            return Collections.singletonList(new UnsupportedTableConstraint(tableName, condExpression));
        }

    }

    public abstract List<TableConstraint> extractConstraints(Connection connectionToDatabase, DbSchemaDto schemaDto) throws SQLException;


}
