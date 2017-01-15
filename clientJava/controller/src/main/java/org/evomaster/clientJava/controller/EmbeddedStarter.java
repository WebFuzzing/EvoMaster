package org.evomaster.clientJava.controller;

import com.ea.agentloader.AgentLoader;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;

import java.lang.reflect.Method;

/**
 * Class used to start the RestController when the SUT
 * is embedded, ie not starting on a separated process.
 */
public class EmbeddedStarter {

    static {
        AgentLoader.loadAgentClass(InstrumentingAgent.class.getName(), "com.");
    }

    //NOTE: following could be refactored, since we init JavaAgent
    private final Object restController;


    public EmbeddedStarter(RestController restController) {

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

        try {
            Method m = restController.getClass().getMethod("startTheControllerServer");
            Boolean b = (Boolean) m.invoke(restController);
            return b;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean stop() {
        try {
            Method m = restController.getClass().getMethod("stopTheControllerServer");
            Boolean b = (Boolean) m.invoke(restController);
            return b;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getControllerServerJettyPort(){
        try {
            Method m = restController.getClass().getMethod("getControllerServerJettyPort");
            Integer port = (Integer) m.invoke(restController);
            return port;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
