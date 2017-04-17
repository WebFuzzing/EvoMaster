package org.evomaster.clientJava.controller;

import com.ea.agentloader.AgentLoader;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;

/**
 * Class used to start the RestController when the SUT
 * is embedded, ie not starting on a separated process.
 */
public class EmbeddedStarter {

    static {
        /*
            note: just passing a valid "com." package at initialization, but that
            ll be modified later
         */
        AgentLoader.loadAgentClass(InstrumentingAgent.class.getName(), "com.");
    }

    private final SutController restController;


    public EmbeddedStarter(SutController restController) {

        this.restController = restController;
        InstrumentingAgent.changePackagesToInstrument(restController.getPackagePrefixesToCover());

        /*
            Note: following cannot work. We need an Agent.
            See InstrumentingClassLoader for more discussion on this problem
         */
//        InstrumentingClassLoader cl = new InstrumentingClassLoader(
//                restController.getPackagePrefixesToCover());
//
//        try {
//            this.restController = cl.loadClass(restController.getClass().getName()).newInstance();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    public boolean start() {

        return restController.startTheControllerServer();
    }

    public boolean stop() {

        return restController.stopTheControllerServer();
    }

    public int getControllerServerJettyPort(){
        return restController.getControllerServerJettyPort();
    }
}
