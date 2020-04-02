package com.foo.rest.examples.spring.openapi.v3.expectations

import com.foo.rest.examples.spring.openapi.v3.base.BaseApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.IllegalArgumentException


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/base"])
open class ExpectationsApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BaseApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun get(success: Boolean) : ResponseEntity<String> {
        if (success) return ResponseEntity.ok("Hello World!!!")
        else throw IllegalArgumentException("Failed Call")
    }
}