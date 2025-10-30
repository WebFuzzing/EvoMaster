package com.foo.rest.examples.spring.openapi.v3.wiremock.auth0

import com.auth0.client.auth.AuthAPI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/wm/auth0"])
class WmAuth0Rest {


    @GetMapping(path = [""])
    fun get() : ResponseEntity<String> {

        try {
            //issue with default 443 on GA
            val domain = "www.doesnotexistfoo.test:6789"
            val audience = String.format("https://%s/api/v2/", domain)
            val authClient = AuthAPI(domain, "foo", "123")

            val tokenHolder = authClient.requestToken(audience).execute()
            return ResponseEntity.ok("OK")
        } catch (e: Exception){
            return ResponseEntity.status(400).build()
        }
    }


}
