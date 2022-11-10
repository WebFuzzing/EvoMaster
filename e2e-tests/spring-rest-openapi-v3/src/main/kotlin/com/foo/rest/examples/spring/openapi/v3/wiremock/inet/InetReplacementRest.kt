package com.foo.rest.examples.spring.openapi.v3.wiremock.inet

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket

@RestController
@RequestMapping(path = ["/api/inet"])
class InetReplacementRest {

    @GetMapping(path = ["/exp/okhttp"])
    fun okHttpExp(): ResponseEntity<String> {
        val address = InetAddress.getByName("imaginary-server.local")

        val socket = Socket(address, 80)
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.inputStream))

        output.println("Hello")
        socket.close()

        return ResponseEntity.ok("OK")
    }
}