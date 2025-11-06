package com.foo.rest.examples.bb.authheader

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbauth"])
@RestController
open class BBAuthApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBAuthApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun get(@RequestHeader("X-FOO") foo: String,
                 @RequestHeader("X-BAR") bar: String,
                 @RequestHeader("Authorization") auth: String,
    ) : ResponseEntity<String> {

        if(foo == "foo" && bar == "42" && auth == "token"){
            CoveredTargets.cover("OK")
            return ResponseEntity.ok("OK")
        }


        return ResponseEntity.status(400).build()
    }
}