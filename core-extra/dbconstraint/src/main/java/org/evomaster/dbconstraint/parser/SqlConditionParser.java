package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.SqlCondition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface SqlConditionParser {

    SqlCondition parse(String sqlConditionStr, ConstraintDatabaseType databaseType) throws SqlConditionParserException;

    /**
     * Parse, but give up if took to long.
     *
     * This is mainly a workaround for possible performance issues in JSqlParser
     */
    default SqlCondition parse(String sqlConditionStr, ConstraintDatabaseType databaseType, long timeoutMs) throws SqlConditionParserException{
        throw new IllegalStateException("Method not implemented");
    }
}
