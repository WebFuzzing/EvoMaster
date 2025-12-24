package com.foo.rest.examples.bb.sqli

import io.swagger.v3.oas.annotations.Operation
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/sqli/query"])
open class BBQuerySQLiApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBQuerySQLiApplication::class.java, *args)
        }
    }

    /**
     * Safe endpoint - No SQL Injection vulnerability
     */
    @GetMapping("/safe")
    @Operation(summary = "Safe Query - No SQL Injection")
    fun getSafeUsers(): ResponseEntity<String?> {
        return ResponseEntity.ok("OK")
    }

    /**
     * Attack: GET /api/sqli/query/vulnerable?username=admin' OR (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--
     */
    @GetMapping("/vulnerable")
    @Operation(summary = "SQL Injection - Query Parameter")
    fun timeBasedQuery(@RequestParam username: String): ResponseEntity<String> {
        if(username.contains("pg_sleep")) {
            //simulating delay
            CoveredTargets.cover("sqli")
            Thread.sleep(5500)
        }

        return ResponseEntity.ok("OK")
    }

}
