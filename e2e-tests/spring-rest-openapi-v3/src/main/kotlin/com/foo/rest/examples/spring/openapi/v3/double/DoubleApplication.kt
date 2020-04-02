package com.foo.rest.examples.spring.openapi.v3.double

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ComponentScan("com.foo.rest.examples.spring.openapi.v3.tomcat")
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/double"])
open class DoubleApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DoubleApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/{x}"])
    open fun get( @PathVariable("x") x: Double) : ResponseEntity<String> {

        if(x > 251.63 && x < 701.69){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}