package com.foo.rest.examples.spring.openapi.v3.externalsref

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/externalsref"])
@RestController
open class ExternalSrefApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
//            SpringApplication.run(ExternalSrefApplication::class.java, *args)
            SpringApplication.run(ExternalSrefApplication::class.java,  "--server.port=10189")
        }
    }

    @GetMapping
    open fun get(
        @RequestParam("a") a: Int,
        @RequestParam("b") b: Int,
        @RequestParam("c") c: Int,
        @RequestParam("d") d: Int,
        @RequestParam("e") e: Int
    ) : ResponseEntity<String> {

        if(a<0 && b<0 && c<0 && d<0 && e<0) {
            return ResponseEntity.ok("OK")
        }
        return ResponseEntity.badRequest().build()
    }
}