package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.SqlCondition;

public interface SqlConditionParser {

    SqlCondition parse(String sqlConditionStr, ConstraintDatabaseType databaseType) throws SqlConditionParserException;
}
