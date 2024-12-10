package org.evomaster.client.java.sql.internal;

public class SqlDistanceWithMetrics {

    public double sqlDistance;

    public int numberOfEvaluatedRows;

    public boolean sqlDistanceEvaluationFailure;

    public SqlDistanceWithMetrics(double sqlDistance, int numberOfEvaluatedRows, boolean sqlDistanceEvaluationFailure) {
        if(sqlDistance < 0){
            throw new IllegalArgumentException("sqlDistance must be non-negative but value is " + sqlDistance);
        }
        if(numberOfEvaluatedRows < 0){
            throw new IllegalArgumentException("numberOfEvaluatedRows must be non-negative but value is " + numberOfEvaluatedRows);
        }
        if (sqlDistanceEvaluationFailure && sqlDistance != Double.MAX_VALUE) {
            throw new IllegalArgumentException("Failed SQL distance computation cannot have a value different than Double.MAX_VALUE");
        }
        this.sqlDistance = sqlDistance;
        this.numberOfEvaluatedRows = numberOfEvaluatedRows;
        this.sqlDistanceEvaluationFailure = sqlDistanceEvaluationFailure;
    }
}
