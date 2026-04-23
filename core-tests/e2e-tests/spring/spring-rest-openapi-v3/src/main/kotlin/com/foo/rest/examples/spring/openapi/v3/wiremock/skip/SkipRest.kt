package com.foo.rest.examples.spring.openapi.v3.wiremock.skip

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.net.URL

@RestController
@RequestMapping(path = ["/api"])
class SkipRest {

    @GetMapping(path = ["/skip"])
    fun skipExternalService(): ResponseEntity<String> {
        val url = URL("http://darpa.test:8080/api/foo")
        val connection = url.openConnection()

        return try {
            val data = connection.getInputStream().bufferedReader().use(BufferedReader::readText)

            if (data == "\"HELLO THERE!!!\""){
                ResponseEntity.status(500).build()
            } else{
                ResponseEntity.ok("OK")
            }
        } catch (e: Exception) {
            ResponseEntity.ok("OK")
        }
    }
}
