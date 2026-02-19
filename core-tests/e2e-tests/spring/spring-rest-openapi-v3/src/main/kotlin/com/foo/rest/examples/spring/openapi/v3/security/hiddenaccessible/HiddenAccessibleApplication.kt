package com.foo.rest.examples.spring.openapi.v3.security.hiddenaccessible

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class HiddenAccessibleApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HiddenAccessibleApplication::class.java, *args)
        }
    }

    @PostMapping(path = ["/resources"])
    open fun post(): ResponseEntity<String>  {
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping(path = ["/resources"])
    open fun get(): ResponseEntity<String>  {
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping(path = ["/resources/{id}"])
    open fun getId(@PathVariable("id") id: Int): ResponseEntity<String> {
        return ResponseEntity.status(200).body("OK")
    }

    @DeleteMapping(path = ["/resources/{id}"])
    open fun deleteId(@PathVariable("id") id: Int): ResponseEntity<String> {
        return ResponseEntity.status(200).body("OK")
    }

}
