package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.locationget

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class HttpInvalidLocationGetApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidLocationGetApplication::class.java, *args)
        }

        fun reset(){
        }
    }


    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<String> {
        return ResponseEntity.status(200).header("Location", "/api/resources/${id + 1000}/notfound").body("Resource with id $id")
    }

    @GetMapping(path = ["/{id}/notfound"])
    open fun getNotFound(@PathVariable("id") id: Int): ResponseEntity<String> {
        return ResponseEntity.status(404).body("Not found")
    }

}
