package com.foo.rest.examples.spring.openapi.v3.wiremock.harveststrategy

import com.fasterxml.jackson.databind.ObjectMapper
import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation.MockResponseDto
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping(path = ["/api/harvest/strategy"])
class HarvestStrategyRest {

    @GetMapping(path = ["/exact"])
    fun getMockExternalResponse(): ResponseEntity<String> {
        val url = URL("http://mock.test:13579/api/mock")

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        val mapper = ObjectMapper()

        return try {
            var response = "Not Working"
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val dto = mapper.readValue(body, MockResponseDto::class.java)
            val message = dto.message

            if (message.equals("Working")) {
                response = "Working"
            }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(500).build()
        }
    }

    @GetMapping(path = ["/closest"])
    fun getMockExternalResponseClosest(): ResponseEntity<String> {
        val url = URL("http://mock.test:13578/api/mock/closest")

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        val mapper = ObjectMapper()

        return try {
            var response = "Not Working"
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val dto = mapper.readValue(body, MockResponseDto::class.java)
            val message = dto.message

            if (message.equals("Working")) {
                response = "Working"
            }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(500).build()
        }
    }

}
