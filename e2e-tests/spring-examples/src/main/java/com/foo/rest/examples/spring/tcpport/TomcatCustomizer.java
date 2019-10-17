package com.foo.rest.examples.spring.tcpport;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Created by arcuri82 on 16-Oct-19.
 */
@Configuration
public class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    /*
        Based on:
        https://stackoverflow.com/questions/31461444/how-do-i-configure-this-property-with-spring-boot-and-an-embedded-tomcat/31461882
     */

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) connector.getProtocolHandler();

            protocol.setMaxKeepAliveRequests(-1);
        });
    }
}