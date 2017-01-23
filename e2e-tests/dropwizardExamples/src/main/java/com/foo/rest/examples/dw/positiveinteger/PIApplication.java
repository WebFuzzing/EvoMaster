package com.foo.rest.examples.dw.positiveinteger;

import io.dropwizard.Application;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Environment;
import io.swagger.jaxrs.config.BeanConfig;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Server;

public class PIApplication extends Application<PIConfiguration> {

    private final int port;
    private Server jettyServer;

    public PIApplication(int port) {
        this.port = port;
    }

    @Override
    public void run(PIConfiguration configuration, Environment environment) throws Exception {

        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new PositiveIntegerRest());

        /*
            very ugly code, but does not seem that Dropwizard gives you any alternative :(
         */
        HttpConnectorFactory applicationConnector = ((HttpConnectorFactory)
                ((DefaultServerFactory) configuration.getServerFactory())
                        .getApplicationConnectors().get(0));
        applicationConnector.setPort(port);

        ((HttpConnectorFactory) ((DefaultServerFactory) configuration.getServerFactory())
                .getAdminConnectors().get(0)).setPort(0);

        environment.lifecycle().addServerLifecycleListener(server -> jettyServer = server);


        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("0.0.1");
        beanConfig.setSchemes(new String[]{"http"});
        //beanConfig.setHost("localhost:8080");
        beanConfig.setBasePath("/api");
        beanConfig.setResourcePackage("com.foo.rest.examples.dw.positiveinteger");
        beanConfig.setScan(true);
        environment.jersey().register(new io.swagger.jaxrs.listing.ApiListingResource());
        environment.jersey().register(new io.swagger.jaxrs.listing.SwaggerSerializers());
    }

    public int getJettyPort(){
        return ((AbstractNetworkConnector)jettyServer.getConnectors()[0]).getLocalPort();
    }

    public Server getJettyServer() {
        return jettyServer;
    }
}
