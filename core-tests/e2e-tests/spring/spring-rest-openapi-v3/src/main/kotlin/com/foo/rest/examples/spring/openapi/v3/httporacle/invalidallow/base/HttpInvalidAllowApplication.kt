package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidallow.base

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class HttpInvalidAllowApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidAllowApplication::class.java, *args)
        }

        private val products = mutableMapOf<Int, String>()
        private val orders = mutableMapOf<Int, String>()

        fun reset() {
            products.clear()
            orders.clear()
        }
    }

    // Faulty resource: DELETE is mapped but hidden from the schema,
    // so OPTIONS Allow lists a verb (DELETE) not documented in OpenAPI.

    @GetMapping(path = ["/products/{id}"])
    open fun getProduct(@PathVariable("id") id: Int): ResponseEntity<String> {
        val value = products[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(value)
    }

    @PutMapping(path = ["/products/{id}"])
    open fun putProduct(@PathVariable("id") id: Int): ResponseEntity<Any> {
        val isNew = !products.containsKey(id)
        products[id] = "$id"
        return ResponseEntity.status(if (isNew) 201 else 200).build()
    }

    @Hidden
    @DeleteMapping(path = ["/products/{id}"])
    open fun deleteProduct(@PathVariable("id") id: Int): ResponseEntity<Any> {
        products.remove(id)
        return ResponseEntity.status(204).build()
    }

    // Clean resource: Allow matches the schema (ignoring HEAD/OPTIONS).

    @GetMapping(path = ["/orders/{id}"])
    open fun getOrder(@PathVariable("id") id: Int): ResponseEntity<String> {
        val value = orders[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(value)
    }

    @PutMapping(path = ["/orders/{id}"])
    open fun putOrder(@PathVariable("id") id: Int): ResponseEntity<Any> {
        val isNew = !orders.containsKey(id)
        orders[id] = "$id"
        return ResponseEntity.status(if (isNew) 201 else 200).build()
    }
}
