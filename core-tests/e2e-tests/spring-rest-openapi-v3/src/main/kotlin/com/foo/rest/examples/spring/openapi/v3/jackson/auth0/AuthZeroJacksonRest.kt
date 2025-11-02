package com.foo.rest.examples.spring.openapi.v3.jackson.auth0

import com.auth0.client.auth.AuthAPI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson"])
class AuthZeroJacksonRest {

    @GetMapping(path = ["/auth"])
    fun get(): ResponseEntity<String> {
        return try {
            val domain = "www.doesnotexistfoo.org:12345"
            val audience = String.format("https://%s/api/v2/", domain)
            val authClient = AuthAPI(domain, "foo", "123")

            val tokenHolder = authClient.requestToken(audience).execute()
            if (tokenHolder.expiresIn > 0) {
                return ResponseEntity.status(200).body("Working")
            }
            return ResponseEntity.status(500).body("Failed")
        } catch (e: Exception) {
            return ResponseEntity.status(500).build()
        }
    }
}