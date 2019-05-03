package org.evomaster.constraint.parser;

import org.evomaster.constraint.ast.SqlCondition;

public abstract class SqlConditionParser {

    public abstract SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException;
}
