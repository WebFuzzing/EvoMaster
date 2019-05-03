package org.evomaster.constraint.parser.jsql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.evomaster.constraint.ast.SqlCondition;
import org.evomaster.constraint.parser.SqlConditionParser;
import org.evomaster.constraint.parser.SqlConditionParserException;

public class JSqlConditionParser extends SqlConditionParser {

    @Override
    public SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException {
        try {
            Expression expression = CCJSqlParserUtil.parseCondExpression(sqlConditionStr, false);
            JSqlVisitor translateToSqlCondition = new JSqlVisitor();
            expression.accept(translateToSqlCondition);
            return translateToSqlCondition.getSqlCondition();
        } catch (JSQLParserException e) {
            throw new SqlConditionParserException(e);
        }
    }
}
