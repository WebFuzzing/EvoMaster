package com.foo.rest.examples.spring.openapi.v3.wiremock.inet

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


@RestController
@RequestMapping(path = ["/api/inet"])
class InetReplacementRest {

    var start = System.currentTimeMillis()

    fun log(msg: String) {
        val now = System.currentTimeMillis()
        val diff = (now - start)
        println("$diff\t$msg")
        start = now
    }

    @GetMapping(path = ["/exp"])
    fun exp(): ResponseEntity<String> {

        //throw 500 by default on exception
        val address = InetAddress.getByName("imaginary-host.local")

        return try {
            val socket = Socket()

            // log("Before address")
            val a = InetSocketAddress(address, 10000)
            // log("Before connect")
            socket.connect(a, 1000)

            val flush = true
            val request = PrintWriter(socket.getOutputStream(), flush)
            val inputStreamReader = BufferedReader(
                InputStreamReader(socket.getInputStream())
            )

            request.println("GET /exp HTTP/1.1")
            request.println("Host: imaginary-host.local:80")
            request.println("Connection: Close")
            request.println()

            // read the response
            var loop = true
            val stringBuilder = StringBuilder(8096)
            while (loop) {
                if (inputStreamReader.ready()) {
                    var i = 0
                    while (i != -1) {
                        i = inputStreamReader.read()
                        stringBuilder.append(i.toChar())
                    }
                    loop = false
                }
            }
            val response = stringBuilder.toString()

            //  log("After connect")
            socket.close()
            //  log("After close")

            if (response.contains("200 OK")) {
                ResponseEntity.ok("OK")
            }

            ResponseEntity.internalServerError().body("ERROR")
        } catch (e: Exception) {
            // log("Exception: ${e.message}")
            ResponseEntity.status(400).build()
        }
    }
}
