package com.foo.rest.examples.spring.openapi.v3.security.xss.reflected

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

// DTO classes for request bodies
data class CommentDto(
    val comment: String? = null,
    val author: String? = null
)

data class MessageDto(
    val message: String? = null,
    val title: String? = null
)

data class FeedbackDto(
    val feedback: String? = null,
    val rating: Int? = null
)

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class XSSReflectedApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XSSReflectedApplication::class.java, *args)
        }
    }

    @Operation(
        summary = "GET endpoint vulnerable to XSS",
        description = "Reflects user input without sanitization - allows XSS attacks"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "HTML response with reflected input")
        ]
    )
    @GetMapping(path = ["/greet"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun greetUser(@RequestParam(name = "name", required = false, defaultValue = "Guest") name: String): String {
        // VULNERABLE: User input is directly embedded into HTML without sanitization
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Greeting Page</title>
            </head>
            <body>
                <h1>Hello, $name!</h1>
                <p>Welcome to our application.</p>
            </body>
            </html>
        """.trimIndent()
    }

    @Operation(
        summary = "POST endpoint vulnerable to XSS",
        description = "Accepts comment data and reflects it without sanitization"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Comment posted successfully")
        ]
    )
    @PostMapping(path = ["/comment"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun postComment(@RequestBody commentDto: CommentDto): String {
        // VULNERABLE: User input from request body is directly embedded into HTML
        val comment = commentDto.comment ?: "No comment"
        val author = commentDto.author ?: "Anonymous"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Comment Posted</title>
            </head>
            <body>
                <h2>Comment Posted Successfully!</h2>
                <div class="comment">
                    <p><strong>Author:</strong> $author</p>
                    <p><strong>Comment:</strong> $comment</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    @Operation(
        summary = "PUT endpoint vulnerable to XSS",
        description = "Updates message and reflects it without sanitization"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Message updated successfully")
        ]
    )
    @PutMapping(path = ["/message"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun updateMessage(@RequestBody messageDto: MessageDto): String {
        // VULNERABLE: User input from request body is directly embedded into HTML
        val title = messageDto.title ?: "Untitled"
        val message = messageDto.message ?: "Empty message"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Message Updated</title>
            </head>
            <body>
                <h1>$title</h1>
                <div class="message">
                    <p>$message</p>
                </div>
                <p><em>Message updated successfully</em></p>
            </body>
            </html>
        """.trimIndent()
    }

    @Operation(
        summary = "PATCH endpoint vulnerable to XSS",
        description = "Partially updates feedback and reflects it without sanitization"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Feedback updated successfully")
        ]
    )
    @PatchMapping(path = ["/feedback"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun patchFeedback(@RequestBody feedbackDto: FeedbackDto): String {
        // VULNERABLE: User input from request body is directly embedded into HTML
        val feedback = feedbackDto.feedback ?: "No feedback provided"
        val rating = feedbackDto.rating ?: 0

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Feedback Updated</title>
            </head>
            <body>
                <h2>Feedback Updated</h2>
                <div class="feedback">
                    <p><strong>Rating:</strong> $rating/5</p>
                    <p><strong>Feedback:</strong> $feedback</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    @Operation(
        summary = "GET endpoint with path parameter vulnerable to XSS",
        description = "Uses path parameter without sanitization - allows XSS attacks"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User profile page")
        ]
    )
    @GetMapping(path = ["/user/{username}"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun getUserProfile(@PathVariable username: String): String {
        // VULNERABLE: Path parameter is directly embedded into HTML without sanitization
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>User Profile</title>
            </head>
            <body>
                <h1>Profile of $username</h1>
                <p>Welcome to $username's profile page!</p>
                <div class="profile-info">
                    <p>Username: $username</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    @Operation(
        summary = "GET endpoint with multiple query parameters vulnerable to XSS",
        description = "Uses multiple query parameters without sanitization"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Search results page")
        ]
    )
    @GetMapping(path = ["/search"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun search(
        @RequestParam(name = "query", required = false, defaultValue = "") query: String,
        @RequestParam(name = "category", required = false, defaultValue = "all") category: String
    ): String {
        // VULNERABLE: Multiple query parameters are directly embedded into HTML
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Search Results</title>
            </head>
            <body>
                <h1>Search Results</h1>
                <p>You searched for: <strong>$query</strong></p>
                <p>In category: <strong>$category</strong></p>
                <div class="results">
                    <p>No results found for "$query" in category "$category"</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
