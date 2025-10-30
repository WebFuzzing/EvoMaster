package com.foo.rest.examples.spring.openapi.v3.jackson.base

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

    @PostMapping(
        path = ["/generic"],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun generic(request: HttpServletRequest): ResponseEntity<String> {

        val json = IOUtils.toString(request.inputStream, StandardCharsets.UTF_8)
        val x: Int
        try {
            val mapper = jacksonObjectMapper()

            val dto = mapper.readValue(json, FooDto::class.java)
            x = dto.x
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body("Failed")
        }
        return if (x > 0) ResponseEntity.ok().body("Working")
        else ResponseEntity.badRequest().body("Failed")
    }
}