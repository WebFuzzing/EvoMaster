package com.foo.rest.examples.spring.openapi.v3.gson.mapdouble

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class MapDoubleGsonApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MapDoubleGsonApplication::class.java, *args)
        }
    }
}
