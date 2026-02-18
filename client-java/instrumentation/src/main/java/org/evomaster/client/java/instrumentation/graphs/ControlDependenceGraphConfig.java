package org.evomaster.client.java.instrumentation.graphs;

import org.evomaster.client.java.utils.SimpleLogger;

/**
 * Runtime configuration for control-dependence graph instrumentation features.
 * Populated via the agent control channel.
 */
public class ControlDependenceGraphConfig {

    private static volatile boolean enableGraphs = false;
    private static volatile boolean writeCfg = false;

    public static boolean isGraphsEnabled() {
        SimpleLogger.debug("enableGraphs: " + enableGraphs);
        return enableGraphs;
    }

    public static void setEnableGraphs(boolean value) {
        SimpleLogger.info("Setting enableGraphs to " + value);
        enableGraphs = value;
    }

    public static boolean isWriteCfgEnabled() {
        SimpleLogger.debug("writeCfg: " + writeCfg);
        return writeCfg;
    }

    public static void setWriteCfgEnabled(boolean value) {
        SimpleLogger.info("Setting writeCfg to " + value);
        writeCfg = value;
    }
}

