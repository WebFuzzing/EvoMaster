package org.evomaster.client.java.sql.internal;

public class SqlDistanceWithMetrics {

    public double sqlDistance;

    public int numberOfEvaluatedRows;

    public SqlDistanceWithMetrics(double sqlDistance, int numberOfEvaluatedRows) {
        if(sqlDistance < 0){
            throw new IllegalArgumentException("sqlDistance must be non-negative but value is " + sqlDistance);
        }
        if(numberOfEvaluatedRows < 0){
            throw new IllegalArgumentException("numberOfEvaluatedRows must be non-negative but value is " + numberOfEvaluatedRows);
        }
        this.sqlDistance = sqlDistance;
        this.numberOfEvaluatedRows = numberOfEvaluatedRows;
    }
}
