package com.foo.rest.examples.spring.openapi.v3.security.ssrf.path

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.HttpURLConnection
import java.net.URL

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class SSRFPathApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SSRFPathApplication::class.java, *args)
        }
    }

    @Operation(
        summary = "GET endpoint to fetch remote image",
        description = "Can be used to fetch remote profile image for user."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful response"),
            ApiResponse(responseCode = "500", description = "Invalid server error")
        ]
    )
    @GetMapping(path = ["/path/{remoteURL}"])
    open fun pathParameter(@PathVariable("remoteURL") remoteURL: String): ResponseEntity<String> {
        if (remoteURL != null) {
            try {
                val url = URL(remoteURL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000

                if (connection.responseCode == 200) {
                    // Do nothing
                    // Usually at this point, the fetched information will be processed
                }
            } catch (e: Exception) {
                // Do nothing
                // Error from the remote service shouldn't impact this endpoint's response
            }
        }

        return ResponseEntity.status(200).body("OK")
    }
}
