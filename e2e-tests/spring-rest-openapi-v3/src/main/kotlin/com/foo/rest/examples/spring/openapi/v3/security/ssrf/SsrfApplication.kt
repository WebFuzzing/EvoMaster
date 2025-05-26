package com.foo.rest.examples.spring.openapi.v3.security.ssrf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class SsrfApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SsrfApplication::class.java, *args)
        }
    }

    @Operation(summary = "POST endpoint to fetch remote image", description = "Can be used to fetch remote profile image for user.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful response"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "500", description = "Invalid server error")
    ])
    @PostMapping(path = ["/fetch/image"])
    open fun fetchUserImage(@RequestBody userInfo: UserDto) : ResponseEntity<String> {
        if (userInfo.userId!!.isNotEmpty() && userInfo.profileImageUrl!!.isNotEmpty()) {

            // If there is a SSRF with file download,
            // the below check shouldn't be there to exploit
            // it further.
            if (!userInfo.profileImageUrl!!.endsWith(".png")) {
                return ResponseEntity.badRequest().body("Invalid profile image type")
            }

            val url = URL(userInfo.profileImageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 1000
            val image = connection.getInputStream().readBytes()

            // Note: Here the saving file should exist

            if (image.isNotEmpty()) {
                return ResponseEntity.status(200).build()
            }
        }

        return ResponseEntity.badRequest().body("Invalid request")
    }

    @Operation(summary = "POST endpoint to fetch sensor data", description = "Can be used to fetch sensor data from remote source")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful response"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "500", description = "Invalid server error")
    ])
    @PostMapping(path = ["/fetch/data"])
    open fun fetchStockData(@RequestBody remoteData: RemoteDataDto): ResponseEntity<String> {
        if (remoteData.sensorUrl!!.isNotEmpty()) {
            val url = URL(remoteData.sensorUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 1000
            val value = connection.getInputStream().readBytes()
            val mapper = ObjectMapper()

            try {
                if (value.isNotEmpty()) {
                    val data = mapper.readValue(value, SensorDataDto::class.java)
                    if (data.temp > 1.0) {
                        return ResponseEntity.status(200).build()
                    }
                }
            } catch (e: Exception) {
                return ResponseEntity.internalServerError().body("Could not fetch the data")
            }

        }
        return ResponseEntity.badRequest().body("Invalid request")
    }
}
