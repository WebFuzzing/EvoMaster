package com.foo.rest.examples.bb.emptybody

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbemptybody"])
@RestController
open class BBEmptyBodyApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBEmptyBodyApplication::class.java, *args)
        }
    }

    private fun verifyHeader(headers: HttpHeaders) {

        /*
            tricky...
            for sure we don't want content-type on empty request, but, in theory,
            we might want to have
            content-length: 0
            to handle some frameworks that might have issues with it.
            but didn't manage to get Jersey to handle it :(
            also, it seems like even if we can control Jersey, the HTTP libraries in the
            generated tests might have their own different "opinions" of what to send...
            what the fucking mess...
            TODO if we find fix, would need to update here
         */

        if (!headers[HttpHeaders.CONTENT_LENGTH].isNullOrEmpty()) {

            headers[HttpHeaders.CONTENT_LENGTH]!!.forEach {
                val size = it.substringAfterLast(':').toInt()
                if(size != 0){
                    throw IllegalArgumentException("Content-Length must be null or 0 value: $headers")
                }
            }
        }
        if (!headers[HttpHeaders.CONTENT_TYPE].isNullOrEmpty()) {
            throw IllegalArgumentException("Content-Length must be null: $headers")
        }
    }

    @PatchMapping(path = ["/patch"])
    fun patch(@RequestHeader headers: HttpHeaders) : ResponseEntity<String> {
        verifyHeader(headers)
        CoveredTargets.cover("PATCH")
        return ResponseEntity.ok("OK")
    }

    @PutMapping(path = ["/put"])
    fun put(@RequestHeader headers: HttpHeaders) : ResponseEntity<String> {
        verifyHeader(headers)
        CoveredTargets.cover("PUT")
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/post"])
    fun post(@RequestHeader headers: HttpHeaders) : ResponseEntity<String> {
        verifyHeader(headers)
        CoveredTargets.cover("POST")
        return ResponseEntity.ok("OK")
    }
}