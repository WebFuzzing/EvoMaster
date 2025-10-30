package com.foo.rest.examples.bb.primitives

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbprimitives"])
@RestController
open class BBPrimitivesApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBPrimitivesApplication::class.java, *args)
        }
    }


    @GetMapping(path = ["/boolean"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getBoolean() : ResponseEntity<Boolean> {
        CoveredTargets.cover("A")
        return ResponseEntity.status(200).body(true)
    }

    @GetMapping(path = ["/int"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getInt() : ResponseEntity<Int> {
        CoveredTargets.cover("B")
        return ResponseEntity.status(200).body(42)
    }

    @GetMapping(path = ["/unquoted"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getUnquoted() : ResponseEntity<String> {
        CoveredTargets.cover("C")
        return ResponseEntity.status(200).body("foo")
    }

    @GetMapping(path = ["/quoted"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getQuoted() : ResponseEntity<String> {
        CoveredTargets.cover("D")
        return ResponseEntity.status(200).body("\"bar\"")
    }

    @GetMapping(path = ["/text"], produces = [MediaType.TEXT_PLAIN_VALUE])
    open fun getText() : ResponseEntity<String> {
        CoveredTargets.cover("E")
        return ResponseEntity.status(200).body("hello there")
    }

}