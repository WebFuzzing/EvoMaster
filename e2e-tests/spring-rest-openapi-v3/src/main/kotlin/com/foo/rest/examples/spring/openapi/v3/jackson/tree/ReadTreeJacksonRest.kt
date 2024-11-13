package com.foo.rest.examples.spring.openapi.v3.jackson.tree

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson/tree"])
class ReadTreeJacksonRest {

    @PostMapping(path = [""], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun post(@RequestBody json : String) : ResponseEntity<String> {
        return try {
            val mapper = ObjectMapper()

            val jsonNode = mapper.readTree(json)

            if (jsonNode.get("name").asText() == "teapot") {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
