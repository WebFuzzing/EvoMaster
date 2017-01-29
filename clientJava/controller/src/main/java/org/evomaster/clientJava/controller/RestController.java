package org.evomaster.clientJava.controller;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.internal.EMControllerApplication;
import org.evomaster.clientJava.controllerApi.ControllerConstants;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class RestController {


    private int controllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT;
    private String controllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST;

    private final EMControllerApplication controllerServer = new EMControllerApplication(this);

    /**
     * Start the controller as a RESTful server.
     * Use the setters of this class to change the default
     * port and host.
     * <br>
     * This method is blocking.
     */
    public boolean startTheControllerServer() {

        try {
            controllerServer.run("server");
        } catch (Exception e) {
            SimpleLogger.error("Failed to start controller server", e);
            return false;
        }

        /*
            Again, very ugly code...
            starting to think if should just get rid off Dropwizard,
            and use directly Jackson with Jetty, eg

            http://nikgrozev.com/2014/10/16/rest-with-embedded-jetty-and-jersey-in-a-single-jar-step-by-step/
         */


        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
        }

        while (!controllerServer.getJettyServer().isStarted()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }

        /*
            TODO: this works ONLY if SUT is running on same process

            TODO: should add REST call from EM to do this reset, eg
                  "initiliazeForNewSearch", at the beginning of each
                  new search
         */
        ObjectiveRecorder.reset();

        return true;
    }

    public boolean stopTheControllerServer() {
        return controllerServer.stopJetty();
    }

    /**
     * @return the actual port in use (eg, if it was an ephemeral 0)
     */
    public int getControllerServerJettyPort() {
        return controllerServer.getJettyPort();
    }


    /**
     * Start a new instance of the SUT.
     * <br>
     * This method must be blocking.
     *
     * @return the base URL of the running SUT, eg "http://localhost:8080"
     */
    public abstract String startSut();

    /**
     * Start a new instance of the SUT, where EvoMaster
     * bytecode instrumentation should be on.
     * <br>
     * This method must be blocking.
     * <br>
     * Note: by default, this method does not do any instrumentation,
     * and just call {@code startSut()}. When the SUT is run on the
     * same process (ie embedded), use {@code EmbeddedStarter}.
     * This method needs to be overwritten only when the SUT is
     * started in a new process.
     *
     * @return the base URL of the running SUT, eg "http://localhost:8080"
     */
    public String startInstrumentedSut(){
        return startSut();
    }

    /**
     * Check if bytecode instrumentation is on.
     * <br>
     * This method needs to be overwritten if SUT is started in
     * a new process.
     * @return
     */
    public boolean isInstrumentationActivated(){
        return InstrumentingAgent.isActive();
    }


    public abstract boolean isSutRunning();

    public abstract void stopSut();

    /**
     * a "," separated list of package prefixes or class names.
     * For example, "com.foo.,com.bar.Bar".
     * Note: be careful of using something as generate as "com."
     * or "org.", as most likely ALL your third-party libraries
     * would be instrumented as well, which could have a severe
     * impact on performance
     *
     * @return
     */
    public abstract String getPackagePrefixesToCover();

    /**
     * A possible (likely inefficient) way to implement this would be to
     * call #stopSUT followed by #startSUT
     */
    public abstract void resetStateOfSUT();

    /**
     * Provide the URL of where the swagger.json can be found
     *
     * @return
     */
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
