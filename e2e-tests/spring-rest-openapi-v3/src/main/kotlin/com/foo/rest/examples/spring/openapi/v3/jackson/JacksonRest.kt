package com.foo.rest.examples.spring.openapi.v3.jackson

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.io.IOUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(path = ["/api/jackson"])
class JacksonRest {

    @PostMapping(path = ["/generic"], consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    open fun generic(request: HttpServletRequest): ResponseEntity<String> {

        val json = IOUtils.toString(request.inputStream, StandardCharsets.UTF_8)
        val mapper = jacksonObjectMapper()

        val dto = mapper.readValue(json, FooDto::class.java)
        return if (dto.x > 0) ResponseEntity.ok().body("Hello World!!!")
        else ResponseEntity.badRequest().body("Failed Call")
    }

    @PostMapping(path = ["/type"], consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    open fun typeReference(request: HttpServletRequest): ResponseEntity<String> {

        val json = IOUtils.toString(request.inputStream, StandardCharsets.UTF_8)
        val mapper = jacksonObjectMapper()

        val dto: FooDto = mapper.readValue(json)
        return if (dto.x > 0) ResponseEntity.ok().body("Hello World!!!")
        else ResponseEntity.badRequest().body("Failed Call")
    }
}


class FooDto(var x: Int)