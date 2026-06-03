package com.foo.rest.examples.bb.httpslocalhost

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
open class BBHttpsLocalhostApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBHttpsLocalhostApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/sayHello"])
    fun sayHello(): ResponseEntity<String> {
        CoveredTargets.cover("OK")
        return ResponseEntity.ok("OK")
    }

}
