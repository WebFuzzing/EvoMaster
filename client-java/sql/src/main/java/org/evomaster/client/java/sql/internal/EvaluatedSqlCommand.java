package org.evomaster.client.java.sql.internal;

/**
 * Created by arcuri82 on 14-Jun-19.
 */
public class EvaluatedSqlCommand {

    public final String sqlCommand;

    public final SqlDistanceWithMetrics sqlDistanceWithMetrics;

    public EvaluatedSqlCommand(String sqlCommand, SqlDistanceWithMetrics sqlDistanceWithMetrics) {
        this.sqlCommand = sqlCommand;
        this.sqlDistanceWithMetrics = sqlDistanceWithMetrics;
    }
}
