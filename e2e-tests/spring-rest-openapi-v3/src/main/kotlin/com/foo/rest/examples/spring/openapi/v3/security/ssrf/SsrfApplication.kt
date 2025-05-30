package com.foo.rest.examples.spring.openapi.v3.security.ssrf

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
import java.net.HttpURLConnection
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
        ApiResponse(responseCode = "201", description = "Successfully fetched remote image"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "500", description = "Invalid server error")
    ])
    @PostMapping(path = ["/fetch/image"])
    open fun fetchUserImage(@RequestBody userInfo: UserDto) : ResponseEntity<String> {
        if (userInfo.userId!!.isNotEmpty() && userInfo.profileImageUrl!!.isNotEmpty()) {
            return try {
                val url = URL(userInfo.profileImageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000

                // Note: Here the saving file should exist
                if (connection.responseCode == 200) {
                    ResponseEntity.status(201).build()
                } else {
                    ResponseEntity.status(200).body("Unable to fetch remote image.")
                }
            } catch (e: Exception) {
                ResponseEntity.internalServerError().body(e.message)
            }
        }

        return ResponseEntity.badRequest().body("Invalid request")
    }

    @Operation(summary = "POST endpoint to fetch sensor data", description = "Can be used to fetch sensor data from remote source")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful response"),
        ApiResponse(responseCode = "201", description = "Successful response when data fetched"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "500", description = "Invalid server error")
    ])
    @PostMapping(path = ["/fetch/data"])
    open fun fetchStockData(@RequestBody remoteData: RemoteDataDto): ResponseEntity<String> {
        if (remoteData.sensorUrl!!.isNotEmpty()) {
            return try {
                val url = URL(remoteData.sensorUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000

                if (connection.responseCode == 200) {
                    ResponseEntity.status(201).build()
                } else {
                    ResponseEntity.status(200).body("Unable to fetch sensor data.")
                }
            } catch (e: Exception) {
                ResponseEntity.internalServerError().body(e.message)
            }
        }

        return ResponseEntity.badRequest().body("Invalid request")
    }
}
