package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/harvester"])
class HarvestOptimisationRest {

    @GetMapping(path = ["/external"])
    fun getMockExternalResponse(): ResponseEntity<String> {

        val url = URL("http://mock.test:9999/api/mock")

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
                val secondURL = URL("http://mock.test:9999/api/mock/second")
                val secondRequest = Request.Builder().url(secondURL).build()
                val secondData = client.newCall(secondRequest).execute()
                val secondBody = secondData.body()?.string()
                val secondDTO = mapper.readValue(secondBody, MockResponseDto::class.java)
                val secondMessage = secondDTO.message
                if (secondMessage.equals("Yes! Working")) {
                    response = "Working"
                }
            }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(500).build()
        }
    }
}
