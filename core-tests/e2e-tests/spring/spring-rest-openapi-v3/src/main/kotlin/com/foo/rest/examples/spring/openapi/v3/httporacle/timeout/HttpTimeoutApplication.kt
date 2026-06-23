package com.foo.rest.examples.spring.openapi.v3.httporacle.timeout

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/timeout"])
@RestController
open class HttpTimeoutApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpTimeoutApplication::class.java, *args)
        }

        fun reset() {}
    }

    @GetMapping(path = ["/slow/{id}"])
    open fun slow(@PathVariable("id") id: Int): ResponseEntity<String> {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(deadline - System.currentTimeMillis())
            } catch (e: InterruptedException) {
                // ignore and keep blocking
            }
        }
        return ResponseEntity.status(200).body("$id")
    }

    // clean
    @GetMapping(path = ["/fast/{id}"])
    open fun fast(@PathVariable("id") id: Int): ResponseEntity<String> {
        return ResponseEntity.status(200).body("$id")
    }
}
