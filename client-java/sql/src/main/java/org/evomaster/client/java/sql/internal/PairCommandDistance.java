package org.evomaster.client.java.sql.internal;

/**
 * Created by arcuri82 on 14-Jun-19.
 */
public class PairCommandDistance {

    public final String sqlCommand;

    public final double distance;

    public final int numberOfFetchedRows;

    public PairCommandDistance(String sqlCommand, double distance) {
        this.sqlCommand = sqlCommand;
        this.distance = distance;
        /**
         * TODO: Add number of fetched rows during the heuristic calculation
         * The statistics will be used to compute the average number of
         * records used to compute the SQL/Mongo heuristic score
         */
        this.numberOfFetchedRows = 0;
    }
}
