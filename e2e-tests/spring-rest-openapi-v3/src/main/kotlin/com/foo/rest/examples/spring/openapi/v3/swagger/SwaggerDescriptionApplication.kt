package com.foo.rest.examples.spring.openapi.v3.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/v1"])
@RestController
open class SwaggerDescriptionApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SwaggerDescriptionApplication::class.java, *args)
        }
    }

    @Operation(summary = "Confirm GET endpoint availability", description = "Returns a success message always.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful response"),
        ApiResponse(responseCode = "500", description = "Invalid server error")
    ])
    @GetMapping
    open fun get() : ResponseEntity<String> {

        return ResponseEntity.ok("GET is working")
    }

    @Operation(summary = "Greet the name of the provided user", description = "Returns a greeting message.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful greeting"),
        ApiResponse(responseCode = "400", description = "Invalid request body")
    ])
    @PostMapping
    open fun post(
        @Valid @RequestBody dto: PersonDto,
        @Parameter(
            description = "Custom header for testing",
            required = false
        )
        @RequestHeader(name = "X-Custom-Header", required = false) customHeader: String
    ): ResponseEntity<String> {

        if (dto.name == "EvoMaster") {
            return ResponseEntity.ok("POST is working")
        }

        return ResponseEntity.badRequest().body("Invalid request body")
    }
}
