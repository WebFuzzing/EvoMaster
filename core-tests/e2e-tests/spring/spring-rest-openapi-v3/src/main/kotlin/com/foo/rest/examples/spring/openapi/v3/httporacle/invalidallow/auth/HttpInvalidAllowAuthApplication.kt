package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidallow.auth

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class HttpInvalidAllowAuthApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidAllowAuthApplication::class.java, *args)
        }

        val USERS = setOf("FOO", "BAR")

        private val products = mutableMapOf<Int, String>()
        private val orders = mutableMapOf<Int, String>()

        fun reset() {
            products.clear()
            orders.clear()
        }
    }

    private fun isValidUser(auth: String?) = auth != null && USERS.contains(auth)

    // OPTIONS is gated behind auth: without a valid user it returns 401, so the Allow
    // header can only be read after the oracle retries with an authenticated user.
    // The Allow lists DELETE, which is not declared in the schema (extra verb) -> fault.
    @RequestMapping(path = ["/products/{id}"], method = [RequestMethod.OPTIONS])
    open fun optionsProduct(
        @RequestHeader(value = "Authorization", required = false) auth: String?
    ): ResponseEntity<Any> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        return ResponseEntity.status(200).header("Allow", "GET,PUT,DELETE,HEAD,OPTIONS").build()
    }

    @GetMapping(path = ["/products/{id}"])
    open fun getProduct(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<String> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        val value = products[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(value)
    }

    @PutMapping(path = ["/products/{id}"])
    open fun putProduct(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        val isNew = !products.containsKey(id)
        products[id] = "$id"
        return ResponseEntity.status(if (isNew) 201 else 200).build()
    }

    // Clean resource: Allow matches the schema (ignoring HEAD/OPTIONS).
    @RequestMapping(path = ["/orders/{id}"], method = [RequestMethod.OPTIONS])
    open fun optionsOrder(
        @RequestHeader(value = "Authorization", required = false) auth: String?
    ): ResponseEntity<Any> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        return ResponseEntity.status(200).header("Allow", "GET,PUT,HEAD,OPTIONS").build()
    }

    @GetMapping(path = ["/orders/{id}"])
    open fun getOrder(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<String> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        val value = orders[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(value)
    }

    @PutMapping(path = ["/orders/{id}"])
    open fun putOrder(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        val isNew = !orders.containsKey(id)
        orders[id] = "$id"
        return ResponseEntity.status(if (isNew) 201 else 200).build()
    }
}
