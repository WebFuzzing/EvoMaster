package com.foo.rest.examples.bb.externalauth

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(path = ["/api/externalauth"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class ExternalAuthApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ExternalAuthApplication::class.java, *args)
        }
    }


    @PostMapping(path = ["/login1"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login(@RequestBody login : LoginDto, response : HttpServletResponse) : ResponseEntity<Map<String, String>> {
        if(login.username == "foo" && login.password == "123"){
            return ResponseEntity.ok(mapOf("access_token" to "token1"))
        }
        return ResponseEntity.status(401).build()
    }

    @PostMapping(path = ["/login2"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login2(@RequestBody login : LoginDto, response : HttpServletResponse) : ResponseEntity<Map<String, String>> {
        if(login.username == "foo" && login.password == "123"){
            return ResponseEntity.ok(mapOf("access_token" to "token2"))
        }
        return ResponseEntity.status(401).build()
    }

    @GetMapping(path = ["/check"])
    open fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String> {

        if(authorization.isNullOrEmpty()){
            return ResponseEntity.status(401).build()
        }
        if(authorization == "token1" || authorization == "token2") {
            CoveredTargets.cover("token1")
            return ResponseEntity.ok(authorization)
        }
        return ResponseEntity.status(401).build()
    }
}