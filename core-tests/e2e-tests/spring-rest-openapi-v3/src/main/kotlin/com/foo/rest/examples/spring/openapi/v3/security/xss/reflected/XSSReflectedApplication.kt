package com.foo.rest.examples.spring.openapi.v3.security.xss.reflected

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

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/reflected"])
@RestController
open class XSSReflectedApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XSSReflectedApplication::class.java, *args)
        }
    }

    // ==== BODY PARAMETER - Comment System ====

    @PostMapping(path = ["/comment"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun reflectComment(@RequestBody commentDto: CommentDto): String {
        // VULNERABLE: Reflects user input without sanitization
        val comment = commentDto.comment ?: "No comment"
        val author = commentDto.author ?: "Anonymous"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Comment Reflected</title>
            </head>
            <body>
                <h2>Comment Received!</h2>
                <div class="comment">
                    <p><strong>Author:</strong> $author</p>
                    <p><strong>Comment:</strong> $comment</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // ==== PATH PARAMETER - User Profile System ====

    @Operation(
        summary = "GET endpoint to display user profile (Reflected XSS with path parameter)",
        description = "Displays user profile without sanitization - allows Reflected XSS attacks via path parameter"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User profile displayed"),
            ApiResponse(responseCode = "400", description = "Invalid URI with special characters")
        ]
    )
    @GetMapping(path = ["/user/{username}"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun getUserProfile(@PathVariable username: String): String {
        // VULNERABLE: Reflects path parameter without sanitization
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>User Profile</title>
            </head>
            <body>
                <h1>Profile of $username</h1>
                <div class="profile-info">
                    <p><strong>Username:</strong> $username</p>
                    <p>Welcome to $username's profile page!</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // ==== QUERY PARAMETER - Search System ====

    @GetMapping(path = ["/search"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun search(
        @RequestParam(name = "query", required = false, defaultValue = "") query: String
    ): String {
        // VULNERABLE: Reflects query parameter without sanitization
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Search Results</title>
            </head>
            <body>
                <h1>Search Results</h1>
                <p>You searched for: <strong>$query</strong></p>
                <div class="results">
                    <p>No results found for "$query"</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
