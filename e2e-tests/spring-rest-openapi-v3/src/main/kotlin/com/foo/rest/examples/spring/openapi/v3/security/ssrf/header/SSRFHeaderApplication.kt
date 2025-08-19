package com.foo.rest.examples.spring.openapi.v3.security.ssrf.header

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.HttpURLConnection
import java.net.URL

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class SSRFHeaderApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SSRFHeaderApplication::class.java, *args)
        }
    }


    /**
     * This is a blind SSRF-example.
     */
    @Operation(
        summary = "POST endpoint to fetch remote image",
        description = "Can be used to fetch remote profile image for user."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful response"),
            ApiResponse(responseCode = "500", description = "Invalid server error")
        ]
    )
    @PostMapping(path = ["/header"])
    open fun headerValue(@RequestHeader("Referer") referer: String): ResponseEntity<String> {
        if (referer != null) {
            try {
                val url = URL(referer)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000

                if (connection.responseCode == 200) {
                    // Do nothing
                }
            } catch (e: Exception) {
                // Do nothing
            }
        }

        return ResponseEntity.ok().build()
    }
}
