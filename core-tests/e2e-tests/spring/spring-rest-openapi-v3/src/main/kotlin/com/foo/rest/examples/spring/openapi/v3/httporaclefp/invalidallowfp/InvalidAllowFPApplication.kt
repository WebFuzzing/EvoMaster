package com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidallowfp

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/invalid-allow-fp"])
@RestController
open class InvalidAllowFPApplication {


    @GetMapping
    open fun get(): ResponseEntity<String> {

        return ResponseEntity.status(200).body("Hello")
    }

    @Hidden
    @DeleteMapping
    open fun deleteProduct(): ResponseEntity<Any> {
        return ResponseEntity.status(204).build()
    }
}