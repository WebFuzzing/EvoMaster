package org.evomaster.clientJava.controller.internal;

import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controllerApi.ControllerConstants;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
import org.evomaster.clientJava.instrumentation.TargetInfo;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class SutController {

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
    public final boolean startTheControllerServer() {


        controllerServer = new Server(InetSocketAddress.createUnresolved(
                getControllerHost(), getControllerPort()));

        ResourceConfig config = new ResourceConfig();
        config.register(JacksonFeature.class);
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

        //just make sure we start from a clean state
        newSearch();

        SimpleLogger.info("Started controller server on: " + controllerServer.getURI());

        return true;
    }

    public final boolean stopTheControllerServer() {
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
    public final int getControllerServerJettyPort() {
        return ((AbstractNetworkConnector) controllerServer.getConnectors()[0]).getLocalPort();
    }


    public final int getControllerPort() {
        return controllerPort;
    }

    public final void setControllerPort(int controllerPort) {
        this.controllerPort = controllerPort;
    }

    public final String getControllerHost() {
        return controllerHost;
    }

    public final void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }


    /**
     * Re-initialize all internal data to enable a completely new search phase
     * which should be independent from previous ones
     */
    public abstract void newSearch();

    /**
     * Re-initialize some internal data needed before running a new test
     */
    public abstract void newTest();

    /**
     * Start a new instance of the SUT.
     * <br>
     * This method must be blocking.
     *
     * @return the base URL of the running SUT, eg "http://localhost:8080"
     */
    public abstract String startSut();


    /**
     * Check if bytecode instrumentation is on.
     * <br>
     * This method needs to be overwritten if SUT is started in
     * a new process.
     *
     * @return
     */
    public abstract boolean isInstrumentationActivated();

    /**
     * Check if the system under test (SUT) is running
     * @return
     */
    public abstract boolean isSutRunning();

    /**
     * Stop the system under test
     */
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
     *
     * @return
     */
    public abstract List<AuthenticationDto> getInfoForAuthentication();



    public abstract List<TargetInfo> getTargetInfos(Collection<Integer> ids);
}
