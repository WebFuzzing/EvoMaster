package org.evomaster.dbconstraint;

import java.util.Objects;

public class UnsupportedTableConstraint extends TableConstraint {

    private final /*non-null*/ String notParserSqlCondition;

    public UnsupportedTableConstraint(String tableName, String notParserSqlCondition) {
        super(tableName);
        this.notParserSqlCondition = Objects.requireNonNull(notParserSqlCondition);
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public String getNotParserSqlCondition() {
        return notParserSqlCondition;
    }
}
