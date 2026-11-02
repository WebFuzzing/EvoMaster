package com.foo.rest.examples.spring.openapi.v3.zerochunk

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.Compression
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.util.unit.DataSize

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class ZeroChunkApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ZeroChunkApplication::class.java, *args)
        }
    }

    // gzip even an empty body, so the client gets a non-empty stream that decodes to nothing
    @Bean
    open fun compression() = WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        it.compression = Compression().apply {
            enabled = true
            mimeTypes = arrayOf("application/json")
            minResponseSize = DataSize.ofBytes(0)
        }
    }
}
