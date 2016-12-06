package org.evomaster.clientJava.controller;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class RestController {

    private int controllerPort = 40100;
    private String controllerHost = "localhost";

    /**
     * Start the controller as a RESTful server.
     * Use the setters of this class to change the default
     * port and host
     */
    public boolean startTheControllerServer(){
        //TODO

        return false;
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
