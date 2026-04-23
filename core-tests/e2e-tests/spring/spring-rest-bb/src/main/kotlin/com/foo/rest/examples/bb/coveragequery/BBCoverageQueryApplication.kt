package com.foo.rest.examples.bb.coveragequery

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping(path = ["/api/coveragequery"])
open class BBCoverageQueryApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBCoverageQueryApplication::class.java, *args)
        }
    }

    @GetMapping
    fun getUser(
        @RequestParam("success", required = true) success: Boolean,
        @RequestParam("a", required = false) a: String?,
        @RequestParam("b", required = false) b: String?,
        @RequestParam("c", required = false) c: String?,
        @RequestParam("d", required = false) d: String?,
        @RequestParam("e", required = false) e: String?
    ) : ResponseEntity<String>{

        CoveredTargets.cover(if(a!=null) "A" else "!A")
        CoveredTargets.cover(if(b!=null) "B" else "!B")
        CoveredTargets.cover(if(c!=null) "C" else "!C")
        CoveredTargets.cover(if(d!=null) "D" else "!D")
        CoveredTargets.cover(if(e!=null) "E" else "!E")

        if(success){
            return ResponseEntity.status(200).build()
        }

        return ResponseEntity.status(400).build()
    }
}