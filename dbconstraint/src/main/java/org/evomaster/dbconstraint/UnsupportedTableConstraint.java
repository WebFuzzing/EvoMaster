package org.evomaster.dbconstraint;

import java.util.Objects;

public class UnsupportedTableConstraint extends TableConstraint {

    private final /*non-null*/ String notParserSqlCondition;

    public UnsupportedTableConstraint(String tableName, String notParserSqlCondition) {
        super(tableName);
        this.notParserSqlCondition = Objects.requireNonNull(notParserSqlCondition);
    }
}
