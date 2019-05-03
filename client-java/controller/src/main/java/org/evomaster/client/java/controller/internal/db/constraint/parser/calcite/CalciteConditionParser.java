package org.evomaster.client.java.controller.internal.db.constraint.parser.calcite;

//import org.apache.calcite.sql.SqlNode;
//import org.apache.calcite.sql.parser.SqlParseException;
//import org.apache.calcite.sql.parser.SqlParser;

import org.evomaster.client.java.controller.internal.db.constraint.expr.SqlCondition;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParser;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParserException;

public class CalciteConditionParser extends SqlConditionParser {

    @Override
    public SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException {
//        SqlParser sqlParser = SqlParser.create(sqlConditionStr);
//        SqlNode sqlNode = null;
//        try {
//            sqlNode = sqlParser.parseExpression();
//        } catch (SqlParseException e) {
//            throw new SqlConditionParserException(e);
//        }
//        CalciteSqlNodeVisitor exprExtractor = new CalciteSqlNodeVisitor();
//        return sqlNode.accept(exprExtractor);
        return null;
    }
}
