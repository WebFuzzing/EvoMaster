package org.evomaster.client.java.sql.internal;

public class SqlDistanceWithMetrics {

    public double sqlDistance;

    public int numberOfEvaluatedRows;

    public boolean sqlDistanceComputacionFailed;

    public SqlDistanceWithMetrics(double sqlDistance, int numberOfEvaluatedRows, boolean sqlDistanceComputacionFailed) {
        if(sqlDistance < 0){
            throw new IllegalArgumentException("sqlDistance must be non-negative but value is " + sqlDistance);
        }
        if(numberOfEvaluatedRows < 0){
            throw new IllegalArgumentException("numberOfEvaluatedRows must be non-negative but value is " + numberOfEvaluatedRows);
        }
        if (sqlDistanceComputacionFailed && sqlDistance != Double.MAX_VALUE) {
            throw new IllegalArgumentException("Failed SQL distance computation cannot have a value different than Double.MAX_VALUE");
        }
        this.sqlDistance = sqlDistance;
        this.numberOfEvaluatedRows = numberOfEvaluatedRows;
        this.sqlDistanceComputacionFailed = sqlDistanceComputacionFailed;
    }
}
