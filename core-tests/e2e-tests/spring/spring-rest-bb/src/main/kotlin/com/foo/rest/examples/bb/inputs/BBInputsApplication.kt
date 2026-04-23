package com.foo.rest.examples.bb.inputs

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbinputs"])
@RestController
open class BBInputsApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBInputsApplication::class.java, *args)
        }
    }

    enum class Foo{
        A, B, C, D, E
    }

    @GetMapping
    open fun get(
        @RequestParam a: Int,
        @RequestParam b: Boolean,
        @RequestParam c: Foo,
        @RequestParam d: String
    ) : ResponseEntity<String> {

        if(a == 42) CoveredTargets.cover("42")
        if(a == 1234) CoveredTargets.cover("1234")
        if(a < 0) CoveredTargets.cover("negative")
        if(b)  CoveredTargets.cover("true")
        if(!b) CoveredTargets.cover("false")
        if(c == Foo.A) CoveredTargets.cover("A")
        if(c == Foo.B) CoveredTargets.cover("B")
        if(c == Foo.C) CoveredTargets.cover("C")
        if(c == Foo.D) CoveredTargets.cover("D")
        if(c == Foo.E) CoveredTargets.cover("E")
        if(d == "foo") CoveredTargets.cover("foo")
        if(d == "bar") CoveredTargets.cover("bar")

        return ResponseEntity.status(200).build()
    }
}