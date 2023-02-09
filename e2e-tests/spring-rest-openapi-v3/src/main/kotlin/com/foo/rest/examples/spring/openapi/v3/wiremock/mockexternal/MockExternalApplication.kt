package com.foo.rest.examples.spring.openapi.v3.wiremock.mockexternal

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/"])
open class MockExternalApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MockExternalApplication::class.java, *args)
        }
    }

    @GetMapping("/")
    open fun index(): ResponseEntity<String> {
        return ResponseEntity.ok("Dummy API Index")
    }

    @GetMapping(path = ["/api/mock"])
    open fun dummyAPI() : ResponseEntity<String> {
        return ResponseEntity.ok("{\"message\":\"Working\"}")
    }
}