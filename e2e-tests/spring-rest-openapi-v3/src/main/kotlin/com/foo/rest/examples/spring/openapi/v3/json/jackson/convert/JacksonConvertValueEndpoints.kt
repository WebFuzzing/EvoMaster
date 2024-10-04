package com.foo.rest.examples.spring.openapi.v3.json.jackson.convert

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson/convert"])
class JacksonConvertValueEndpoints {

    @PostMapping(path = ["/map"])
    fun post(@RequestBody json : String?) : ResponseEntity<String> {
        return try {
            val mapper = ObjectMapper()
//            val c = mapOf(
//                "name" to "teapot",
//                "model" to "x"
//            )

            val car = mapper.readValue(json, Map::class.java)

            val result = mapper.convertValue(car, CarDto::class.java)

            if (result.name == "teapot") {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build()
            }
            ResponseEntity.ok(json)
        } catch (ex : Exception) {
            ResponseEntity.badRequest().build()
        }
    }
}
