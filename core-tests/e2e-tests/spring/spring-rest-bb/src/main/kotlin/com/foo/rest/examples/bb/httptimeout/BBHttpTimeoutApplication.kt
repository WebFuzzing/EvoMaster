package com.foo.rest.examples.bb.httptimeout

import org.evomaster.e2etests.utils.CoveredTargets
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
open class BBHttpTimeoutApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBHttpTimeoutApplication::class.java, *args)
        }
    }

    // slow endpoint: blocks longer than the client timeout, triggering a HTTP_TIMEOUT fault.
    // the target is covered as soon as the request is handled, before the client gives up.
    @GetMapping(path = ["/slow/{id}"])
    open fun slow(@PathVariable("id") id: Int): ResponseEntity<String> {
        CoveredTargets.cover("timeout")
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
