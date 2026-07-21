package com.foo.rest.examples.spring.openapi.v3.examplepool

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/examplepool"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class ExamplePoolApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ExamplePoolApplication::class.java, *args)
        }
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun post(@RequestBody dto: ExamplePoolDto) : ResponseEntity<String>{

        if(dto.x== "foo" && dto.y== "bar"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}

