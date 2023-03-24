package com.foo.rest.examples.spring.openapi.v3.wiremock.jackson

import com.auth0.client.auth.AuthAPI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/jackson/wm"])
open class JacksonRest {

    @GetMapping(path = [""])
    fun get() : ResponseEntity<String> {

        try {
            val domain = "www.doesnotexistfoo.org:9000"
            val audience = String.format("https://%s/api/v2/", domain)
            val authClient = AuthAPI(domain, "foo", "123")

            val tokenHolder = authClient.requestToken(audience).execute()
            return ResponseEntity.ok("OK")
        } catch (e: Exception){
            return ResponseEntity.status(400).build()
        }
    }
}