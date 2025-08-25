package org.evomaster.client.java.controller.internal.db;

public class OpenSearchCommandWithDistance {
    public final Object command;

    public final OpenSearchDistanceWithMetrics distanceWithMetrics;

    public OpenSearchCommandWithDistance(Object command, OpenSearchDistanceWithMetrics distanceWithMetrics) {
        this.command = command;
        this.distanceWithMetrics = distanceWithMetrics;
    }

    public Object getCommand() {
        return command;
    }

    public OpenSearchDistanceWithMetrics getDistanceWithMetrics() {
        return distanceWithMetrics;
    }
}
