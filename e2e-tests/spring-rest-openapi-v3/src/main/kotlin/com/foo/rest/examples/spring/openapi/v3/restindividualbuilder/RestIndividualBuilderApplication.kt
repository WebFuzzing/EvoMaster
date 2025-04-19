package com.foo.rest.examples.spring.openapi.v3.restindividualbuilder;

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/"])
@RestController
open class RestIndividualBuilderApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(RestIndividualBuilderApplication::class.java, *args)
        }
    }

    @PostMapping(path = ["{id}/x"])
    open fun create(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {
        return ResponseEntity.status(201).build<Any>()
    }

    @PutMapping(path = ["{id}/x"])
    open fun change(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {
        return ResponseEntity.status(201).build()
    }

    @GetMapping(path = ["{id}/x"])
    open fun get(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {
        return ResponseEntity.status(201).build()
    }
}