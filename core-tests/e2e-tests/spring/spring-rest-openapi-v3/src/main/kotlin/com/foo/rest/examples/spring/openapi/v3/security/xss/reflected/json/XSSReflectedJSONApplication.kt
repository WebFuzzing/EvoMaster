package com.foo.rest.examples.spring.openapi.v3.security.xss.reflected.json

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

data class CommentDto(
    val comment: String? = null,
    val author: String? = null
)

data class CommentResponseDto(
    val message: String,
    val author: String,
    val comment: String
)

data class UserProfileDto(
    val username: String,
    val welcomeMessage: String
)

data class SearchResultDto(
    val query: String,
    val message: String
)

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/reflected/json"])
@RestController
open class XSSReflectedJSONApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XSSReflectedJSONApplication::class.java, *args)
        }
    }

    // ==== BODY PARAMETER - Comment System ====

    @PostMapping(path = ["/comment"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun reflectComment(@RequestBody commentDto: CommentDto): CommentResponseDto {
        // VULNERABLE: Reflects user input without sanitization in JSON response
        val comment = commentDto.comment ?: "No comment"
        val author = commentDto.author ?: "Anonymous"

        return CommentResponseDto(
            message = "Comment Received!",
            author = author,
            comment = comment
        )
    }

    // ==== PATH PARAMETER - User Profile System ====

    @Operation(
        summary = "GET endpoint to display user profile (Reflected XSS with path parameter)",
        description = "Displays user profile without sanitization - returns user input in JSON"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User profile displayed"),
            ApiResponse(responseCode = "400", description = "Invalid URI with special characters")
        ]
    )
    @GetMapping(path = ["/user/{username}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getUserProfile(@PathVariable username: String): UserProfileDto {
        // VULNERABLE: Reflects path parameter without sanitization in JSON response
        return UserProfileDto(
            username = username,
            welcomeMessage = "Welcome to $username's profile page!"
        )
    }

    // ==== QUERY PARAMETER - Search System ====

    @GetMapping(path = ["/search"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun search(
        @RequestParam(name = "query", required = false, defaultValue = "") query: String
    ): SearchResultDto {
        // VULNERABLE: Reflects query parameter without sanitization in JSON response
        return SearchResultDto(
            query = query,
            message = "No results found for \"$query\""
        )
    }
}
