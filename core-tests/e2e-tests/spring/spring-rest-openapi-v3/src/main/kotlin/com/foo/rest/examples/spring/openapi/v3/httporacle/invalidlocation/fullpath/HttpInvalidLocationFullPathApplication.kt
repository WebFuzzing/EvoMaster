package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.fullpath

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class HttpInvalidLocationFullPathApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidLocationFullPathApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }


    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<String> {
        val value = data[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(value)
    }


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {

        val isNew = !data.containsKey(id)
        data[id] = "$id"

        val location = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/resources/{id}")
            .buildAndExpand(id + 1000)
            .toUri()

        val status = if (isNew) 201 else 200
        return ResponseEntity.status(status)
            .header("Location", location.toString())
            .build()
    }
}
