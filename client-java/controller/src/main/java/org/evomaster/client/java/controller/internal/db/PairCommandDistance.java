package org.evomaster.client.java.controller.internal.db;

/**
 * Created by arcuri82 on 14-Jun-19.
 */
public class PairCommandDistance {

    public final String sqlCommand;

    public final double distance;


    public PairCommandDistance(String sqlCommand, double distance) {
        this.sqlCommand = sqlCommand;
        this.distance = distance;
    }
}
