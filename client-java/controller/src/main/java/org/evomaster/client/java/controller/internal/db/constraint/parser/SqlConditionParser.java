package org.evomaster.client.java.controller.internal.db.constraint.parser;

import org.evomaster.client.java.controller.internal.db.constraint.expr.SqlCondition;

public abstract class SqlConditionParser {

    public abstract SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException;
}
