package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.ast.SqlCondition;

public interface SqlConditionParser {

    SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException;
}
