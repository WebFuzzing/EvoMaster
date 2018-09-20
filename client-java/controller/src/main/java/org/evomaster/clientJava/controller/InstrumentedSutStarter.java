package org.evomaster.clientJava.controller;

import com.ea.agentloader.AgentLoader;
import org.evomaster.clientJava.controller.internal.SutController;
import org.evomaster.clientJava.controller.internal.db.StandardOutputTracker;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;

/**
 * Class responsible to handle the SutController in a way
 * in which bytecode instrumentation is activated.
 * Note: instrumentation is needed when generating tests
 * with EvoMaster, but not when we run the generated tests.
 */
public class InstrumentedSutStarter {

    static {
        /*
            Force loading of Agent here, just to make sure it is called only once

            note: just passing a valid package at initialization, but that
            ll be modified later if needed.
         */
        AgentLoader.loadAgentClass(InstrumentingAgent.class.getName(), "foobar_packagenameshouldnotexist.");
    }

    private final SutController sutController;


    public InstrumentedSutStarter(SutController sutController) {

        this.sutController = sutController;

        if (sutController instanceof EmbeddedSutController) {
            InstrumentingAgent.changePackagesToInstrument(sutController.getPackagePrefixesToCover());

            String driver = sutController.getDatabaseDriverName();
            if(driver!=null && ! driver.isEmpty()){
                InstrumentingAgent.initP6Spy(driver);
                return;
            }

        } else if(sutController instanceof ExternalSutController){
            ((ExternalSutController)sutController).setInstrumentation(true);
            /*
                Reducing amount of logging from Jetty.
                Note: this is not done for embedded one, as that might conflict with
                the SUT if the SUT is using Jetty.
             */
            System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
            System.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        } else {
            throw new IllegalArgumentException("Invalid SUT controller type");
        }
    }

    public boolean start() {
        StandardOutputTracker.setTracker(true, sutController);
        return sutController.startTheControllerServer();
    }

    public boolean stop() {
        StandardOutputTracker.setTracker(false, null);
        return sutController.stopTheControllerServer();
    }

    public int getControllerServerPort() {
        return sutController.getControllerServerPort();
    }

}
