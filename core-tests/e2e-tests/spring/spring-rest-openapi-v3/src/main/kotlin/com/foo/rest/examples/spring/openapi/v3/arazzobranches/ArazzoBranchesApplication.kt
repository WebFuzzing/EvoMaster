package com.foo.rest.examples.spring.openapi.v3.arazzobranches

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class ArazzoBranchesApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ArazzoBranchesApplication::class.java, *args)
        }
    }
}
