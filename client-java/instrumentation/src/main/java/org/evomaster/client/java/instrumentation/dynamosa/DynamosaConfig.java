package org.evomaster.client.java.instrumentation.dynamosa;

/**
 * Runtime configuration for DYNAMOSA-related instrumentation features.
 * Populated via the agent control channel.
 */
public class DynamosaConfig {

    private static volatile boolean enableGraphs = false;
    private static volatile boolean writeCfg = false;

    public static boolean isGraphsEnabled() {
        return enableGraphs;
    }

    public static void setEnableGraphs(boolean value) {
        enableGraphs = value;
    }

    public static boolean isWriteCfgEnabled() {
        return writeCfg;
    }

    public static void setWriteCfgEnabled(boolean value) {
        writeCfg = value;
    }
}


