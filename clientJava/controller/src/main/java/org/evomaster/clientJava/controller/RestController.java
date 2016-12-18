package org.evomaster.clientJava.controller;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.internal.EMControllerApplication;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class RestController {

    private int controllerPort = 40100;
    private String controllerHost = "localhost";

    private final EMControllerApplication controllerServer = new EMControllerApplication(this);

    /**
     * Start the controller as a RESTful server.
     * Use the setters of this class to change the default
     * port and host
     */
    public boolean startTheControllerServer(){

        try {
            controllerServer.run("server");
        } catch (Exception e) {
            SimpleLogger.error("Failed to start controller server", e);
            return false;
        }

        /*
            Again, very ugly code...
            starting to think if should just get rid off Dropwizard,
            and use directly Jackson with Jetty
         */

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
        }

        while(! controllerServer.getJettyServer().isStarted()){
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }

        return true;
    }

    public boolean stopTheControllerServer(){
        return controllerServer.stopJetty();
    }

    /**
     *
     * @return the actual port in use (eg, if it was an ephemeral 0)
     */
    public int getControllerServerJettyPort(){
        return controllerServer.getJettyPort();
    }


    /**
     * Start a new instance of the SUT
     *
     * @return the URL of base path of the running SUT
     */
    public abstract String startSut();

    public abstract String startInstrumentedSut();

    public abstract boolean isSutRunning();

    public abstract void stopSut();

    /**
     * A possible (likely inefficient) way to implement this would be to
     * call #stopSUT followed by #startSUT
     */
    public abstract void resetStateOfSUT();

    public abstract String getUrlOfSwaggerJSON();


    public int getControllerPort() {
        return controllerPort;
    }

    public void setControllerPort(int controllerPort) {
        this.controllerPort = controllerPort;
    }

    public String getControllerHost() {
        return controllerHost;
    }

    public void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }
}
