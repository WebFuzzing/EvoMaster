package com.foo.rest.examples.spring.openapi.v3.wiremock.socketconnect.okhttp3

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class WmSocketConnectApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(WmSocketConnectApplication::class.java, *args)
        }
    }
}