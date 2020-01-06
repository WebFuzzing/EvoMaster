package com.foo.rest.examples.spring.openapi.v3.base

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/base"])
open class BaseApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BaseApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun get() : ResponseEntity<String> {

        return ResponseEntity.ok("Hello World!!!")
    }
}