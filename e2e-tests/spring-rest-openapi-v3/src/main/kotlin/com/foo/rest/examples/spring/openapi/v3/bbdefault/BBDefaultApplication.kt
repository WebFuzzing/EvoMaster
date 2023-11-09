package com.foo.rest.examples.spring.openapi.v3.bbdefault

import com.foo.rest.examples.spring.openapi.v3.base.BaseApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbdefault"])
@RestController
open class BBDefaultApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBDefaultApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun get(@RequestParam data: String?) : ResponseEntity<String> {

        if(data == "foo")
            return ResponseEntity.ok("OK")

        return ResponseEntity.status(400).build()
    }
}