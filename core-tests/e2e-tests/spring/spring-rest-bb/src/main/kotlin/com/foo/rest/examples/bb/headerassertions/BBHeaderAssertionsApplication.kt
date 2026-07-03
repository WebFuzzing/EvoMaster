package com.foo.rest.examples.bb.headerassertions

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping(path = ["/api/headerassertions"])
open class BBHeaderAssertionsApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBHeaderAssertionsApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/ok/401"])
    fun ok401(): ResponseEntity<String>{
        CoveredTargets.cover("ok401")
        return ResponseEntity.status(401).header("WWW-Authenticate","some value").build()
    }

    @GetMapping(path = ["/fail/401"])
    fun fail401(): ResponseEntity<String>{
        CoveredTargets.cover("fail401")
        return ResponseEntity.status(401).build()
    }

    @GetMapping(path = ["/ok/405"])
    fun ok405(): ResponseEntity<String>{
        CoveredTargets.cover("ok405")
        return ResponseEntity.status(405).header("Allow","POST,OPTIONS").build()
    }

    @GetMapping(path = ["/fail/405"])
    fun fail405(): ResponseEntity<String>{
        CoveredTargets.cover("fail405")
        return ResponseEntity.status(405).build()
    }

    @GetMapping(path = ["/ok/426"])
    fun ok426(): ResponseEntity<String>{
        CoveredTargets.cover("ok426")
        return ResponseEntity.status(426).header("Upgrade","foo").build()
    }

    @GetMapping(path = ["/fail/426"])
    fun fail426(): ResponseEntity<String>{
        CoveredTargets.cover("fail426")
        return ResponseEntity.status(426).build()
    }
}
