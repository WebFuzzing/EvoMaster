package com.foo.rest.examples.spring.openapi.v3.jackson.mapint

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
@RequestMapping(path = ["/api/jackson/mapint"])
class MapIntJacksonRest {

    private val mapper = jacksonObjectMapper()

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun post(request: HttpServletRequest): ResponseEntity<String> {

        val json = IOUtils.toString(request.inputStream, StandardCharsets.UTF_8)
        val collection = mapper.readValue(json, Map::class.java)
        val x : Int = collection["x"] as Int

        return if (x > 0) ResponseEntity.ok().body("Working")
        else ResponseEntity.badRequest().body("Failed")
    }
}