package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/harvester"])
class HarvestOptimisationRest {


    @Value("\${external}")
    private var externalURL: String? = null

    @GetMapping(path = ["/external"])
    fun getMockExternalResponse(): ResponseEntity<String> {


        val url = URL("${externalURL}/api/mock")

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        val mapper = ObjectMapper()

        return try {
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val code = data.code()
            val dto = mapper.readValue(body, MockResponseDto::class.java)
            if (code != 200)
                return ResponseEntity.status(400).build()
            val message = dto.message
            val msg = "${if (message.equals("Working")) "Working" else "Not Working"}"
            ResponseEntity.ok(msg)
        } catch (e: Exception) {
            ResponseEntity.status(500).build()
        }
    }
}