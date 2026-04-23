package com.foo.rest.examples.spring.openapi.v3.tomcat

import org.apache.catalina.connector.Connector
import org.apache.coyote.http11.AbstractHttp11Protocol
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Configuration

/**
 * Created by arcuri82 on 03-Mar-20.
 */
@Configuration
open class TomcatCustomizer : WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    /*
        Based on:
        https://stackoverflow.com/questions/31461444/how-do-i-configure-this-property-with-spring-boot-and-an-embedded-tomcat/31461882
     */
    override fun customize(factory: TomcatServletWebServerFactory) {
        factory.addConnectorCustomizers(TomcatConnectorCustomizer { connector: Connector ->
            val protocol = connector.protocolHandler as AbstractHttp11Protocol<*>
            protocol.setMaxKeepAliveRequests(-1)
        })
    }
}