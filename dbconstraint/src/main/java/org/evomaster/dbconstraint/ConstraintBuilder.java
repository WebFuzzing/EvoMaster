package org.evomaster.dbconstraint;

import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.extract.SqlConditionTranslator;
import org.evomaster.dbconstraint.extract.TranslationContext;
import org.evomaster.dbconstraint.parser.SqlConditionParser;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;
import org.evomaster.dbconstraint.parser.SqlConditionParserFactory;

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
