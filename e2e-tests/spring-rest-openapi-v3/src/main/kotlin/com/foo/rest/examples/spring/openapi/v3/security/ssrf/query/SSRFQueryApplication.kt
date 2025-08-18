package com.foo.rest.examples.spring.openapi.v3.security.ssrf.query

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class SSRFQueryApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SSRFQueryApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/query"])
    open fun queryValue(): ResponseEntity<String> {

        return ResponseEntity.ok().build()
    }
}
