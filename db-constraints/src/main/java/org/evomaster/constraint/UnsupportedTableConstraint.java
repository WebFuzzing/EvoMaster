package org.evomaster.constraint;

public class UnsupportedTableConstraint extends TableConstraint {

    private final String notParserSqlCondition;

    public UnsupportedTableConstraint(String tableName, String notParserSqlCondition) {
        super(tableName);
        this.notParserSqlCondition = notParserSqlCondition;
    }
}
