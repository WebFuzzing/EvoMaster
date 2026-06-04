package com.foo.rest.examples.bb.advancedformats

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/advancedformats"])
@RestController
open class BBAdvancedFormatsApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBAdvancedFormatsApplication::class.java, *args)
        }
    }


    @GetMapping("/uuid")
    open fun getUuid(
        @RequestParam(required = true) x: String?
    ) : ResponseEntity<String> {

        if (x == null) {
            return ResponseEntity.status(400).build()
        }

        UUID.fromString(x)

        CoveredTargets.cover("uuid")

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/email")
    open fun getEmail(
        @RequestParam(required = true) x: String?
    ) : ResponseEntity<String> {

        if (x == null || !x.contains('@')) {
            return ResponseEntity.status(400).build()
        }

        CoveredTargets.cover("email")

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/uri")
    open fun getUri(
        @RequestParam(required = true) x: String?
    ) : ResponseEntity<String> {

        if (x == null) {
            return ResponseEntity.status(400).build()
        }

        URI(x)

        CoveredTargets.cover("uri")

        return ResponseEntity.status(200).body("OK")
    }

}