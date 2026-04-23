package com.foo.rest.examples.spring.openapi.v3.wiremock.socket

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

@RestController
@RequestMapping(path = ["/api/"])
class SocketRest {

    @GetMapping(path = ["/resolve"])
    fun exp(): ResponseEntity<String> {
        val a = InetSocketAddress("imaginary-socket-evomaster.test", 12345)

        return try {

            val socket = Socket()

            socket.connect(a)
            socket.close()

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(400).build()
        }
    }
}
