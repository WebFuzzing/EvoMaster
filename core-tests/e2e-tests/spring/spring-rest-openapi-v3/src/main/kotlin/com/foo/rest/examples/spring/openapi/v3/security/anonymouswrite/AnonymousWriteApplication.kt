package com.foo.rest.examples.spring.openapi.v3.security.anonymouswrite

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class AnonymousWriteApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(AnonymousWriteApplication::class.java, *args)
        }
    }
    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")

    @PostMapping
    open fun post(
    ): ResponseEntity<Any> {
        return ResponseEntity.status(201).build()
    }

    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        return ResponseEntity.status(204).build()
    }

    @PatchMapping(path = ["/{id}"])
    open fun patch(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        return ResponseEntity.status(200).build()
    }

    @DeleteMapping(path = ["/{id}"])
    open fun delete(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        return ResponseEntity.status(204).build()
    }

    @GetMapping(path = ["/{id}"])
    open fun get(
        @PathVariable("id") id: Int
    ): ResponseEntity<String> {
        return ResponseEntity.status(200).body("OK")
    }

    @PutMapping(path = ["/201/{id}"])
    open fun put201(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        return ResponseEntity.status(201).build()
    }

    //Authentication

    @PutMapping(path = ["/auth/{id}"])
    open fun putAuth(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.status(204).build()
    }

    @PatchMapping(path = ["/auth/{id}"])
    open fun patchAuth(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.status(200).build()
    }

    @DeleteMapping(path = ["/auth/{id}"])
    open fun deleteAuth(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.status(204).build()
    }

}
