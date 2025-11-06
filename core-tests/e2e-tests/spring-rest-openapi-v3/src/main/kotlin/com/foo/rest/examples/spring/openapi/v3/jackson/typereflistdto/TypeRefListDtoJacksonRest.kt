package com.foo.rest.examples.spring.openapi.v3.jackson.typereflistdto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.io.IOUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping(path = ["/api/jackson/typereflistdto"])
class TypeRefListDtoJacksonRest {

    private val mapper = jacksonObjectMapper()

    private class Dto(
        var x : Int? = null,
    )


    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun post(@RequestBody json: String): ResponseEntity<String> {

        val collection = mapper.readValue(json, object:TypeReference<ArrayList<Dto>>() {})
        val dto = collection[0]
        val x = dto.x!!

        return if (x > 0) ResponseEntity.ok().body("Working")
        else ResponseEntity.badRequest().body("Failed")
    }
}