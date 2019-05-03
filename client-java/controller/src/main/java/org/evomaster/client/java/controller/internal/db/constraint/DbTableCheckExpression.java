package org.evomaster.client.java.controller.internal.db.constraint;

/**
 * A Sql CHECK expression on a table of the schema
 */
public class DbTableCheckExpression extends DbTableConstraint {

    private final /*non-null*/ String sqlCheckExpression;


    public DbTableCheckExpression(String tableName, String sqlCheckExpression) {
        super(tableName);
        if (sqlCheckExpression == null) {
            throw new IllegalArgumentException("check expression cannot be null");
        }
        this.sqlCheckExpression = sqlCheckExpression;
    }

    public String getSqlCheckExpression() {
        return sqlCheckExpression;
    }

}
