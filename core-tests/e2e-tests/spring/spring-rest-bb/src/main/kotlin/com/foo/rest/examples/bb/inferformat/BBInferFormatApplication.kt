package com.foo.rest.examples.bb.inferformat

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/inferformat"])
@RestController
open class BBInferFormatApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBInferFormatApplication::class.java, *args)
        }
    }


    @GetMapping("/uuid")
    open fun getUuid(
        @RequestParam(required = true) uuid: String?
    ) : ResponseEntity<String> {

        if (uuid == null) {
            return ResponseEntity.status(400).build()
        }

        UUID.fromString(uuid)

        CoveredTargets.cover("uuid")

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/email")
    open fun getEmail(
        @RequestParam(required = true) theEmail: String?
    ) : ResponseEntity<String> {

        if (theEmail == null || !theEmail.contains('@') || !theEmail.contains('.')) {
            return ResponseEntity.status(400).build()
        }

        CoveredTargets.cover("email")

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/uri")
    open fun getUri(
        @RequestParam(required = true) addressUri: String?
    ) : ResponseEntity<String> {

        if (addressUri == null) {
            return ResponseEntity.status(400).build()
        }

        URI(addressUri)

        CoveredTargets.cover("uri")

        return ResponseEntity.status(200).body("OK")
    }


    @PostMapping("/uuid")
    open fun postUuid(
        @RequestBody dto: BBInferFormatDto
    ) : ResponseEntity<String> {

        UUID.fromString(dto.foo)

        CoveredTargets.cover("description-uuid")

        return ResponseEntity.status(200).body("OK")
    }

    @PostMapping("/date")
    open fun postDate(
        @RequestBody dto: BBInferFormatDto
    ) : ResponseEntity<String> {

        LocalDateTime.parse(dto.bar!!)

        CoveredTargets.cover("description-date")

        return ResponseEntity.status(200).body("OK")
    }
}