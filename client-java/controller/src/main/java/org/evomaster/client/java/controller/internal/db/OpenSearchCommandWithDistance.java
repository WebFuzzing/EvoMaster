package org.evomaster.client.java.controller.internal.db;

public class OpenSearchCommandWithDistance {
    private final Object command;

    private final OpenSearchDistanceWithMetrics distanceWithMetrics;

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
