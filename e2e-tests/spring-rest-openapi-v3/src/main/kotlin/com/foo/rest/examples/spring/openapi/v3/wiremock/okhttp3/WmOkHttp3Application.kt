package com.foo.rest.examples.spring.openapi.v3.wiremock.okhttp3

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class WmOkHttp3Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(WmOkHttp3Application::class.java, *args)
        }
    }
}