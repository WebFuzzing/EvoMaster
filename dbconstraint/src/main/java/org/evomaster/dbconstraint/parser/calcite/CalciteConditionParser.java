package org.evomaster.dbconstraint.parser.calcite;

//import org.apache.calcite.sql.SqlNode;
//import org.apache.calcite.sql.parser.SqlParseException;
//import org.apache.calcite.sql.parser.SqlParser;

import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.parser.SqlConditionParser;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;

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
