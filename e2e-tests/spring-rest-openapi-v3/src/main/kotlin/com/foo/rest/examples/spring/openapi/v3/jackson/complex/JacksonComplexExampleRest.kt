package com.foo.rest.examples.spring.openapi.v3.jackson.complex

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson/complex"])
class JacksonComplexExampleRest {

    private val objectMapper = ObjectMapper()

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun complexExample(@RequestBody json: String): ResponseEntity<String> {
        val person = objectMapper.readValue(json, PersonDto::class.java)

        val results = objectMapper.convertValue(
            person.contact.contactElements,
            object : TypeReference<ArrayList<ContactElementDto>>() {})

        val contactElement: ContactElementDto = results[0]

        if (contactElement.id == 5553456) {
            return ResponseEntity.ok("Working")
        }

        return ResponseEntity.badRequest().body("Failed")
    }
}
