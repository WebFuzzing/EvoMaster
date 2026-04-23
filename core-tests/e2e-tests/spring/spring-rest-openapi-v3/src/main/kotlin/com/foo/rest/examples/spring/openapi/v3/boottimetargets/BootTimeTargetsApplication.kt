package com.foo.rest.examples.spring.openapi.v3.boottimetargets

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class BootTimeTargetsApplication {

    companion object{
        @JvmStatic
        fun main(args: Array<String>){
            SpringApplication.run(BootTimeTargetsApplication::class.java, *args)
        }
    }
}