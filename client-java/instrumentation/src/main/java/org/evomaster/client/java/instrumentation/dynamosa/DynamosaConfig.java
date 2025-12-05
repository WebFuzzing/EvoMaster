/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa;

/**
 * Runtime configuration for DYNAMOSA-related instrumentation features.
 * Populated via the agent control channel.
 */
public class DynamosaConfig {

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


