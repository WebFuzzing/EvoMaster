package org.evomaster.client.java.sql.internal;

public class SqlDistanceWithMetrics {

    public double sqlDistance;

    public int numberOfEvaluatedRows;

    public SqlDistanceWithMetrics(double sqlDistance, int numberOfEvaluatedRows) {
        this.sqlDistance = sqlDistance;
        this.numberOfEvaluatedRows = numberOfEvaluatedRows;
    }
}
