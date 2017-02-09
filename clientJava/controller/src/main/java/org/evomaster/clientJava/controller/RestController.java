package org.evomaster.clientJava.controller;

import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.internal.EMController;
import org.evomaster.clientJava.controllerApi.ControllerConstants;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class RestController {


    private int controllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT;
    private String controllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST;

    private Server controllerServer;

    /**
     * Start the controller as a RESTful server.
     * Use the setters of this class to change the default
     * port and host.
     * <br>
     * This method is blocking until the server is initialized.
     */
    public boolean startTheControllerServer() {


        controllerServer = new Server(InetSocketAddress.createUnresolved(
                getControllerHost(), getControllerPort()));

        ResourceConfig config = new ResourceConfig();
        config.register(new EMController(this));

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        ServletContextHandler context = new ServletContextHandler(controllerServer,
                ControllerConstants.BASE_PATH + "/*");
        context.addServlet(servlet, "/*");


        try {
            controllerServer.start();
        } catch (Exception e) {
            SimpleLogger.error("Failed to start Jetty: " + e.getMessage());
            controllerServer.destroy();
        }

        /*
            TODO: this works ONLY if SUT is running on same process
         */
        ObjectiveRecorder.reset();

        SimpleLogger.info("Started controller server on: "+controllerServer.getURI());

        return true;
    }

    public boolean stopTheControllerServer() {
        try {
            controllerServer.stop();
            return true;
        } catch (Exception e) {
            SimpleLogger.error(e.toString());
            return false;
        }
    }

    /**
     * @return the actual port in use (eg, if it was an ephemeral 0)
     */
    public int getControllerServerJettyPort() {
        return ((AbstractNetworkConnector) controllerServer.getConnectors()[0]).getLocalPort();
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
    public String startInstrumentedSut() {
        return startSut();
    }

    /**
     * Check if bytecode instrumentation is on.
     * <br>
     * This method needs to be overwritten if SUT is started in
     * a new process.
     *
     * @return
     */
    public boolean isInstrumentationActivated() {
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

    /**
     * Provide a list of valid authentication credentials, or {@code null} if
     * none is necessary
     * @return
     */
    public abstract List<AuthenticationDto> getInfoForAuthentication();


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
