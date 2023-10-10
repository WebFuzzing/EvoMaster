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

    var start = System.currentTimeMillis()

    fun log(msg: String){
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
          //  log("After connect")
            socket.close()
          //  log("After close")

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
           // log("Exception: ${e.message}")
            ResponseEntity.status(400).build()
        }
    }
}