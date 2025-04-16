package com.foo.rest.examples.spring.openapi.v3.security.wronglydesignedput

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/wronglydesignedput/resources"])
@RestController
open class SecurityWronglyDesignedPut {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SecurityWronglyDesignedPut::class.java, *args)
        }
    }


    @PostMapping
    open fun create(
        @RequestHeader("Authorization") auth: String?,
    ) : ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        return ResponseEntity.status(200).build<Any>()
    }

    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")


    @DeleteMapping(path = ["/{id}"])
    open fun delete(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(auth == "BAR"){
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.status(204).build()
    }

    @PutMapping(path = ["/{id}"])
    open fun put(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        return ResponseEntity.status(404).build()
    }

}