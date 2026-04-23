package com.foo.rest.examples.spring.openapi.v3.jackson.liststring

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson/list/string"])
class ListStringJacksonRest {

    private val objectMapper = ObjectMapper()

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun postList(@RequestBody json: String): ResponseEntity<String> {
        // Sample JSON: ["teapot","cup"]
        val arrayList = objectMapper.readValue(json, ArrayList::class.java)
//        val keyword: String = arrayList[1] as String

        return if (arrayList.contains("teapot")) {
            ResponseEntity.ok("Working")
        } else {
            ResponseEntity.badRequest().body("Failed")
        }
    }
}
