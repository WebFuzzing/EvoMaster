package org.evomaster.constraint;

import org.evomaster.constraint.ast.SqlCondition;
import org.evomaster.constraint.extract.SqlConditionTranslator;
import org.evomaster.constraint.extract.TranslationContext;
import org.evomaster.constraint.parser.SqlConditionParser;
import org.evomaster.constraint.parser.SqlConditionParserException;
import org.evomaster.constraint.parser.SqlConditionParserFactory;

public class ConstraintBuilder {

    public TableConstraint translateToConstraint(String tableName, String condExpression) {

        SqlConditionParser sqlParser = SqlConditionParserFactory.buildParser();
        try {
            SqlCondition expr = sqlParser.parse(condExpression);
            TranslationContext translationContext = new TranslationContext(tableName);
            SqlConditionTranslator exprExtractor = new SqlConditionTranslator(translationContext);
            TableConstraint constraint = expr.accept(exprExtractor, null);
            return constraint;
        } catch (SqlConditionParserException e) {
            return new UnsupportedTableConstraint(tableName, condExpression);
        }

    }
}
