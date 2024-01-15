package com.foo.rest.examples.spring.openapi.v3.wiremock.urlopen

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/urlopen"])
class WmUrlOpenRest {

    @GetMapping(path = ["/string"])
    fun getString() : ResponseEntity<String> {

        val url = URL("http://hello.there:8123/api/string")
        val connection = url.openConnection()
        connection.setRequestProperty("accept", "application/json")
        val data = connection.getInputStream().bufferedReader().use(BufferedReader::readText)

        return if (data == "\"HELLO THERE!!!\""){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }
    }


    @GetMapping(path = ["/object"])
    fun getObject() : ResponseEntity<String> {

        val url = URL("http://hello.there:8877/api/object")
        val connection = url.openConnection()
        connection.setRequestProperty("accept", "application/json")

        val mapper = ObjectMapper()
        val dto = mapper.readValue(connection.getInputStream(), WmUrlOpenDto::class.java)

        return if (dto.x!! > 0){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }
    }

    @GetMapping(path = ["/sstring"])
    fun getSString() : ResponseEntity<String> {

        val url = URL("https://hello.there:8443/api/string")
        val connection = url.openConnection()
        connection.setRequestProperty("accept", "application/json")
        val data = connection.getInputStream().bufferedReader().use(BufferedReader::readText)

        return if (data == "\"HELLO THERE!!!\""){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }
    }

}
