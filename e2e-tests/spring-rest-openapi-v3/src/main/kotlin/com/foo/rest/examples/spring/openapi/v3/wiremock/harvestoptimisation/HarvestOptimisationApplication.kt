package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/wm/harvester"])
open class HarvestOptimisationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HarvestOptimisationApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/external"])
    fun getMockExternalResponse(): ResponseEntity<String> {
        val url = URL("http://localhost:65530/api/mock")

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
            val msg = "$code:${if (message.equals("Working")) "WORKING" else "Not Working"}"
            ResponseEntity.ok(msg)
        } catch (e: Exception) {
            ResponseEntity.status(500).build()
        }
    }
}