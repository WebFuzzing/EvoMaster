package com.foo.rest.examples.spring.openapi.v3.security.xss.stored.html

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
@RequestMapping(path = ["/api/stored"])
@RestController
open class XSSStoredApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XSSStoredApplication::class.java, *args)
        }

        // In-memory storage for stored XSS examples
        private val comments = mutableListOf<Pair<String, String>>() // Body parameter
        private val userBios = mutableMapOf<String, String>() // Path parameter
        private val guestbookEntries = mutableListOf<Pair<String, String>>() // Query parameter
    }

    // ==== BODY PARAMETER - Comment System ====

    @PostMapping(path = ["/comment"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun storeComment(@RequestBody commentDto: CommentDto): String {
        // VULNERABLE: Stores user input without sanitization
        val comment = commentDto.comment ?: "No comment"
        val author = commentDto.author ?: "Anonymous"

        comments.add(Pair(author, comment))

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Comment Stored</title>
            </head>
            <body>
                <h2>Comment Stored Successfully!</h2>
                <p>Your comment has been saved and will be displayed to other users.</p>
                <a href="/api/stored/comments">View all comments</a>
            </body>
            </html>
        """.trimIndent()
    }

    @GetMapping(path = ["/comments"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun getComments(): String {
        // VULNERABLE: Displays stored user input without sanitization
        val commentsList = comments.joinToString("\n") { (author, comment) ->
            """
                <div class="comment">
                    <p><strong>Author:</strong> $author</p>
                    <p><strong>Comment:</strong> $comment</p>
                    <hr>
                </div>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>All Comments</title>
            </head>
            <body>
                <h1>All Comments</h1>
                ${if (comments.isEmpty()) "<p>No comments yet.</p>" else commentsList}
            </body>
            </html>
        """.trimIndent()
    }

    // ==== PATH PARAMETER - User Bio System ====

    @Operation(
        summary = "POST endpoint to store user bio (Stored XSS with path parameter)",
        description = "Stores user bio in memory without sanitization - allows Stored XSS attacks via path parameter"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Bio stored successfully"),
            ApiResponse(responseCode = "400", description = "Invalid URI with special characters")
        ]
    )
    @PostMapping(path = ["/user/{username}"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun storeBio(
        @PathVariable username: String,
        @RequestParam(name = "bio", required = false, defaultValue = "") bio: String
    ): String {
        // VULNERABLE: Stores user input from both path parameter and query parameter without sanitization
        userBios[username] = bio

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Bio Stored</title>
            </head>
            </html>
        """.trimIndent()
    }

    @Operation(
        summary = "GET endpoint to retrieve user profile with bio (Stored XSS)",
        description = "Displays stored user bio without sanitization - executes stored XSS from path parameter data"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User profile displayed"),
            ApiResponse(responseCode = "400", description = "Invalid URI with special characters")
        ]
    )
    @GetMapping(path = ["/user/{username}"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun getUserProfile(@PathVariable username: String): String {
        // VULNERABLE: Displays stored user input without sanitization
        val bio = userBios[username] ?: "No bio available"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>User Profile</title>
            </head>
            <body>
                <div class="profile-info">
                    <p><strong>Bio:</strong> $bio</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // ==== QUERY PARAMETER - Guestbook System ====

    @PostMapping(path = ["/guestbook"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun storeGuestbookEntry(
        @RequestParam(name = "name", required = false, defaultValue = "Anonymous") name: String,
        @RequestParam(name = "entry", required = false, defaultValue = "") entry: String
    ): String {
        // VULNERABLE: Stores user input from query parameters without sanitization
        guestbookEntries.add(Pair(name, entry))

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Entry Stored</title>
            </head>
            <body>
                <h2>Guestbook Entry Stored!</h2>
                <p>Thank you for signing our guestbook!</p>
                <a href="/api/stored/guestbook">View guestbook</a>
            </body>
            </html>
        """.trimIndent()
    }

    @GetMapping(path = ["/guestbook"], produces = [MediaType.TEXT_HTML_VALUE])
    open fun getGuestbook(): String {
        // VULNERABLE: Displays stored user input without sanitization
        val entriesList = guestbookEntries.joinToString("\n") { (name, entry) ->
            """
                <div class="entry">
                    <p><strong>$name</strong> wrote:</p>
                    <p>$entry</p>
                    <hr>
                </div>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Guestbook</title>
            </head>
            <body>
                <h1>Guestbook</h1>
                ${if (guestbookEntries.isEmpty()) "<p>No entries yet. Be the first to sign!</p>" else entriesList}
            </body>
            </html>
        """.trimIndent()
    }
}
