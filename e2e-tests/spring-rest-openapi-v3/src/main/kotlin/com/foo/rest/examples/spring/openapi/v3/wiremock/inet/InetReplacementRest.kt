package com.foo.rest.examples.spring.openapi.v3.wiremock.inet

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

@RestController
@RequestMapping(path = ["/api/inet"])
class InetReplacementRest {

    @GetMapping(path = ["/exp"])
    fun exp(): ResponseEntity<String> {
        val address = InetAddress.getByName("imaginary-host.local")
        // This will refer to the same IP which got replaced at the Inet replacement level
        // while there will be a WM running on a different IP
        // So need to initialise the WM on the first replaced IP
        // checking based on the signature has to be changed at the socket level
        // The signature should be consistent through out the whole flow
        return try {
            val socket = Socket()

            socket.connect(InetSocketAddress(address, 10000), 1000)
            socket.close()

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(500).build()
        }
    }
}