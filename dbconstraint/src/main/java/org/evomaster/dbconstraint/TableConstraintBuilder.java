package org.evomaster.dbconstraint;

import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.extract.SqlCannotBeTranslatedException;
import org.evomaster.dbconstraint.extract.SqlConditionTranslator;
import org.evomaster.dbconstraint.extract.TranslationContext;
import org.evomaster.dbconstraint.parser.SqlConditionParser;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;
import org.evomaster.dbconstraint.parser.SqlConditionParserFactory;

public class TableConstraintBuilder {

    public TableConstraint translateToConstraint(String tableName, String condExpression, ConstraintDatabaseType databaseType) {

        SqlConditionParser sqlParser = SqlConditionParserFactory.buildParser();
        SqlCondition expr;
        try {
            expr = sqlParser.parse(condExpression);
        } catch (SqlConditionParserException ex) {
            return new UnsupportedTableConstraint(tableName, condExpression);
        }

        TranslationContext translationContext = new TranslationContext(tableName, databaseType);
        SqlConditionTranslator exprExtractor = new SqlConditionTranslator(translationContext);
        try {
            return expr.accept(exprExtractor, null);
        } catch (SqlCannotBeTranslatedException ex) {
            return new UnsupportedTableConstraint(tableName, condExpression);
        }
    }
}
