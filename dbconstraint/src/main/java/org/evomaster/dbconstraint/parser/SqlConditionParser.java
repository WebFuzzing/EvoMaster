package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.ast.SqlCondition;

public abstract class SqlConditionParser {

    public abstract SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException;
}
