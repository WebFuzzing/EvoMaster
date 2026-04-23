package com.foo.rest.examples.spring.openapi.v3.namedexample

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.ws.rs.QueryParam

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/namedexample"])
@RestController
open class NamedExampleApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(NamedExampleApplication::class.java, *args)
        }
    }


    @PostMapping
    open fun post(
        @QueryParam("q0") q0: String?,
        @QueryParam("q1") q1: String?,
        @QueryParam("q2") q2: String?,
        @QueryParam("q3") q3: String?,
        @QueryParam("q4") q4: String?,
        @RequestBody dto: NamedExampleDto
    ) : ResponseEntity<String> {

        //on purpose do nothing with input... should still be selected in test suites due to BB coverage

        return ResponseEntity.ok("OK")
    }
}
