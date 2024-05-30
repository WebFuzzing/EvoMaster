package com.foo.rest.examples.spring.openapi.v3.wiremock.canonical

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class InetCanonicalApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<InetCanonicalApplication>(*args)
        }
    }
}
