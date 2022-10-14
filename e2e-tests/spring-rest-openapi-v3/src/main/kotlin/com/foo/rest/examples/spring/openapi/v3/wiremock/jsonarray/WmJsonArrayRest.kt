package com.foo.rest.examples.spring.openapi.v3.wiremock.jsonarray

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/jsonarray"])
class WmJsonArrayRest {


    @GetMapping
    open fun getObject() : ResponseEntity<String> {

        val url = URL("http://json.array:10877/api/foo")
        val connection = url.openConnection()
        connection.setRequestProperty("accept", "application/json")

        val mapper = ObjectMapper()
        val list = mapper.readValue(connection.getInputStream(), ArrayList::class.java)

        val dto = mapper.convertValue(list[0], WmJsonArrayDto::class.java)

        return if (dto.x!! > 0){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }
    }
}