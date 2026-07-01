package com.foo.rest.examples.bb.bodyunsupported

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/bodyunsupported"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class BBBodyUnsupportedApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBBodyUnsupportedApplication::class.java, *args)
        }
    }


    @PostMapping(path = ["/octets"], consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun postOctets(@RequestBody body: String) : ResponseEntity<String> {

        if(body.isEmpty()){
            return ResponseEntity.status(400).build()
        }

        CoveredTargets.cover("OCTETS")
        return ResponseEntity.ok().body("OK")
    }

    @PostMapping(path = ["/pdf"], consumes = [MediaType.APPLICATION_PDF_VALUE])
    fun postPdf(@RequestBody body: String) : ResponseEntity<String> {
        if(body.isEmpty()){
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("PDF")
        return ResponseEntity.ok().body("OK")
    }

}