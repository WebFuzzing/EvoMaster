package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class HarvestOptimisationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HarvestOptimisationApplication::class.java, *args)
        }
    }
}