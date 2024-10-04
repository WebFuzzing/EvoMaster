package com.foo.rest.examples.spring.openapi.v3.json.jackson.tree

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson/tree"])
class JacksonReadTreeEndpoints {

    @PostMapping(path = [""])
    fun post(@RequestBody json : String?) : ResponseEntity<String> {
        return try {
            val mapper = ObjectMapper()

            val result = mapper.readTree(json)

            if (result.get("name").asText() == "teapot") {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build()
            }

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
