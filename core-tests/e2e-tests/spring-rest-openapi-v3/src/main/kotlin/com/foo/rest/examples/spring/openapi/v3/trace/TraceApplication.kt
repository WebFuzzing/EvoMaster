package com.foo.rest.examples.spring.openapi.v3.trace

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/trace"])
@RestController
open class TraceApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(TraceApplication::class.java, *args)
        }
    }

    @RequestMapping(method = [RequestMethod.TRACE])
    open fun trace() : ResponseEntity<String> {
        return ResponseEntity.ok().build()
    }
}