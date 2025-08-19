package com.foo.rest.examples.spring.openapi.v3.security.ssrf.header

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
     * https://bugzilla.redhat.com/show_bug.cgi?id=2145254
     * Referer header can be used to trace the user activity.
     * In some cases, applications call the referer for some purposes.
     * Shellshock is another example of such use.
     * https://portswigger.net/web-security/ssrf/blind/lab-shellshock-exploitation
     * https://medium.com/@muhammadosama0121/server-side-request-forgery-ssrf-41275201e79c
     */
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
    @GetMapping(path = ["/header"])
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

        return ResponseEntity.status(200).body("OK")
    }
}
