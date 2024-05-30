package com.foo.rest.examples.spring.openapi.v3.wiremock.canonical

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress

@RestController
@RequestMapping(path = arrayOf("/canonical"))
class InetCanonicalApplicationRest {

    @GetMapping(path = arrayOf("/test"))
    fun test(): ResponseEntity<String> {
        val address = InetAddress.getByName("localhost")

        val canonical = InetAddress.getByName(address.toString());
        canonical.canonicalHostName
        return ResponseEntity.ok("OK")
    }
}
