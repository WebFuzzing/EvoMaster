package org.evomaster.clientJava.controller;

import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;

import java.lang.reflect.Method;

/**
 * Class used to start the RestController when the SUT
 * is embedded, ie not starting on a separated process.
 */
public class EmbeddedStarter {

    private final Object restController;


    public EmbeddedStarter(RestController restController) {

        InstrumentingClassLoader cl = new InstrumentingClassLoader(
                restController.getPackagePrefixesToCover());

        try {
            this.restController = cl.loadClass(restController.getClass().getName()).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
