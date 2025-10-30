package com.foo.rest.examples.spring.openapi.v3.wiremock.hostnameaction

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

@RestController
@RequestMapping(path = ["/api/"])
class HostnameResolutionActionRest {

    @GetMapping(path = ["/resolve"])
    fun exp(): ResponseEntity<String> {
        val address = InetAddress.getByName("imaginary-second.local")

        return try {
            val socket = Socket()

            val a = InetSocketAddress(address, 10000)
            socket.connect(a, 10000)
            socket.close()

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(400).build()
        }
    }
}
