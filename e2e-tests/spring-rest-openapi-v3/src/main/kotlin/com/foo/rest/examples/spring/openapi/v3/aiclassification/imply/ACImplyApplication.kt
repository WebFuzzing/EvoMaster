package com.foo.rest.examples.spring.openapi.v3.aiclassification.imply

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class ACImplyApplication {

    companion object{
        @JvmStatic
        fun main(args: Array<String>){
            SpringApplication.run(ACImplyApplication::class.java, *args)
        }
    }
}