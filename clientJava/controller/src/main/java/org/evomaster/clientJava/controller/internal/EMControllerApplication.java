package org.evomaster.clientJava.controller.internal;

import io.dropwizard.Application;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.Server;
import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.RestController;

public class EMControllerApplication extends Application<EMControllerConfiguration> {

    private final RestController restController;

    /**
     * Instance of Jetty server used by Dropwizard
     */
    private Server jettyServer;


    public EMControllerApplication(RestController restController) {
        this.restController = restController;
    }

    @Override
    public String getName() {
        return "Remote REST controller for EvoMaster";
    }


    @Override
    public void run(EMControllerConfiguration configuration, Environment environment) throws Exception {

        environment.jersey().setUrlPattern("/controller/api/*");

        /*
            very ugly code, but does not seem that Dropwizard gives you any alternative :(
         */
        HttpConnectorFactory applicationConnector = ((HttpConnectorFactory)
                ((DefaultServerFactory) configuration.getServerFactory())
                .getApplicationConnectors().get(0));
        applicationConnector.setPort(restController.getControllerPort());
        applicationConnector.setBindHost(restController.getControllerHost());

        ((HttpConnectorFactory) ((DefaultServerFactory) configuration.getServerFactory())
                .getAdminConnectors().get(0)).setPort(0);

        environment.lifecycle().addServerLifecycleListener(server -> jettyServer = server);
    }

    public boolean stopJetty(){
        try {
            jettyServer.stop();
        } catch (Exception e) {
            SimpleLogger.error("Failed to stop Jetty for Dropwizard", e);
            return false;
        }
        return true;
    }
}