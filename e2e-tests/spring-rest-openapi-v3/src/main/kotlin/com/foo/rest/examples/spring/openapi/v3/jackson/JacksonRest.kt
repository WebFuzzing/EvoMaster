package com.foo.rest.examples.spring.openapi.v3.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.io.IOUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

//    @GetMapping(path = ["/auth"])
//    fun get(): ResponseEntity<String> {
//            val domain = "www.doesnotexistfoo.org:9000"
//            val audience = String.format("https://%s/api/v2/", domain)
//            val authClient = AuthAPI(domain, "foo", "123")
//
//            val tokenHolder = authClient.requestToken(audience).execute()
//            return ResponseEntity.ok("OK")
//    }

    @GetMapping(path = ["/byte/{s}"])
    fun byteArrayExample(@PathVariable("s") s: String): ResponseEntity<String> {
        val sampleJSON = "{\"message\":\"%s\"}".format(s)

        val objectMapper = ObjectMapper()
        val response = objectMapper.readValue(sampleJSON.toByteArray(), DummyResponse::class.java)

        if (response.message != null && response.message.equals("foo")) {
            return ResponseEntity.status(200).body("Working")
        }
        return ResponseEntity.status(200).body("Failed")
    }
}