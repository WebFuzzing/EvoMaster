package org.evomaster.client.java.instrumentation.graphs;

/**
 * Runtime configuration for control-dependence graph instrumentation features.
 * Populated via the agent control channel.
 */
public class ControlDependenceGraphConfig {

    private static volatile boolean enableGraphs = false;
    private static volatile boolean writeCfg = false;

    
    public static boolean isGraphsEnabled() {
        System.out.println("enableGraphs: " + enableGraphs);
        return enableGraphs;
    }

    public static void setEnableGraphs(boolean value) {
        System.out.println("----------------------------------------------");
        System.out.println("Setting enableGraphs to " + value);
        System.out.println("----------------------------------------------");
        enableGraphs = value;
    }

    public static boolean isWriteCfgEnabled() {
        System.out.println("----------------------------------------------");
        System.out.println("writeCfg: " + writeCfg);
        System.out.println("----------------------------------------------");
        return writeCfg;
    }

    public static void setWriteCfgEnabled(boolean value) {
        System.out.println("----------------------------------------------");
        System.out.println("Setting writeCfg to " + value);
        System.out.println("----------------------------------------------");
        writeCfg = value;
    }
}

