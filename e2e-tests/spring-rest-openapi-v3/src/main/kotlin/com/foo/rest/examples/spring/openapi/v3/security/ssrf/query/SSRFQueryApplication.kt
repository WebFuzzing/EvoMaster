package com.foo.rest.examples.spring.openapi.v3.security.ssrf.query

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.HttpURLConnection
import java.net.URL

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class SSRFQueryApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SSRFQueryApplication::class.java, *args)
        }
    }

    @Operation(
        summary = "GET endpoint to fetch data from remote source",
        description = "Can be used to fetch data from remote source."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful response"),
            ApiResponse(responseCode = "204", description = "No data to fetch"),
            ApiResponse(responseCode = "400", description = "Invalid request"),
            ApiResponse(responseCode = "500", description = "Invalid server error")
        ]
    )
    @GetMapping(path = ["/query"])
    open fun queryValue(@RequestParam dataSource: String): ResponseEntity<String> {
        if (dataSource != null) {
            return try {
                val url = URL(dataSource)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000

                if (connection.responseCode == 200) {
                    return ResponseEntity.status(200).body("OK")
                }
                ResponseEntity.status(204).build()
            } catch (e: Exception) {
                ResponseEntity.status(204).body("Unable to fetch remote image.")
            }
        }

        return ResponseEntity.badRequest().body("Invalid request")
    }
}
