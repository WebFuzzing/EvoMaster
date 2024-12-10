package org.evomaster.client.java.sql.internal.constraint;

import java.util.Objects;

/**
 * A Sql CHECK expression on a table of the schema
 */
public class DbTableCheckExpression extends DbTableConstraint {

    private final /*non-null*/ String sqlCheckExpression;


    public DbTableCheckExpression(String tableName, String sqlCheckExpression) {
        super(tableName);
        this.sqlCheckExpression = Objects.requireNonNull(sqlCheckExpression);
    }

    public String getSqlCheckExpression() {
        return sqlCheckExpression;
    }

}
