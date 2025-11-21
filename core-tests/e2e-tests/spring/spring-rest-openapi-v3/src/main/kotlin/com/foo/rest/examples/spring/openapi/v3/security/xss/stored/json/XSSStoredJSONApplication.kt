package com.foo.rest.examples.spring.openapi.v3.security.xss.stored.json

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class CommentDto(
    val comment: String? = null,
    val author: String? = null
)

data class CommentResponseDto(
    val message: String,
    val success: Boolean = true
)

data class CommentsListDto(
    val comments: List<CommentItemDto>
)

data class CommentItemDto(
    val author: String,
    val comment: String
)

data class BioResponseDto(
    val message: String,
    val success: Boolean = true
)

data class UserProfileDto(
    val username: String,
    val bio: String
)

data class GuestbookResponseDto(
    val message: String,
    val success: Boolean = true
)

data class GuestbookListDto(
    val entries: List<GuestbookEntryDto>
)

data class GuestbookEntryDto(
    val name: String,
    val entry: String
)

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/stored/json"])
@RestController
open class XSSStoredJSONApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XSSStoredJSONApplication::class.java, *args)
        }

        // In-memory storage for stored XSS examples
        private val comments = mutableListOf<Pair<String, String>>() // Body parameter
        private val userBios = mutableMapOf<String, String>() // Path parameter
        private val guestbookEntries = mutableListOf<Pair<String, String>>() // Query parameter
    }

    // ==== BODY PARAMETER - Comment System ====

    @PostMapping(path = ["/comment"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun storeComment(@RequestBody commentDto: CommentDto): ResponseEntity<CommentResponseDto> {
        // VULNERABLE: Stores user input without sanitization
        val comment = commentDto.comment ?: "No comment"
        val author = commentDto.author ?: "Anonymous"

        comments.add(Pair(author, comment))

        val response = CommentResponseDto(
            message = "Comment Stored Successfully! Your comment has been saved and will be displayed to other users.",
            success = true
        )

        return ResponseEntity(response, HttpStatus.OK)
    }

    @GetMapping(path = ["/comments"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getComments(): ResponseEntity<CommentsListDto> {
        // VULNERABLE: Displays stored user input without sanitization
        val commentsList = comments.map { (author, comment) ->
            CommentItemDto(author = author, comment = comment)
        }

        val response = CommentsListDto(comments = commentsList)
        return ResponseEntity(response, HttpStatus.OK)
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
    @PostMapping(path = ["/user/{username}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun storeBio(
        @PathVariable username: String,
        @RequestParam(name = "bio", required = false, defaultValue = "") bio: String
    ): ResponseEntity<BioResponseDto> {
        // VULNERABLE: Stores user input from both path parameter and query parameter without sanitization
        userBios[username] = bio

        val response = BioResponseDto(
            message = "Bio stored successfully for user",
            success = true
        )

        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "GET endpoint to retrieve user profile with bio (Stored XSS)",
        description = "Displays stored user bio without sanitization - executes stored XSS from path parameter data"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "User profile displayed"),
            ApiResponse(responseCode = "400", description = "Invalid URI with special characters")
        ]
    )
    @GetMapping(path = ["/user/{username}"])
    open fun getUserProfile(@PathVariable username: String): ResponseEntity<UserProfileDto> {
        // VULNERABLE: Displays stored user input without sanitization
        val bio = userBios[username] ?: "No bio available"

        val response = UserProfileDto(
            username = username,
            bio = bio
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return ResponseEntity(response, headers,HttpStatus.OK)
    }

    // ==== QUERY PARAMETER - Guestbook System ====

    @PostMapping(path = ["/guestbook"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun storeGuestbookEntry(
        @RequestParam(name = "name", required = false, defaultValue = "Anonymous") name: String,
        @RequestParam(name = "entry", required = false, defaultValue = "") entry: String
    ): ResponseEntity<GuestbookResponseDto> {
        // VULNERABLE: Stores user input from query parameters without sanitization
        guestbookEntries.add(Pair(name, entry))

        val response = GuestbookResponseDto(
            message = "Guestbook Entry Stored! Thank you for signing our guestbook!",
            success = true
        )

        return ResponseEntity(response, HttpStatus.OK)
    }

    @GetMapping(path = ["/guestbook"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getGuestbook(): ResponseEntity<GuestbookListDto> {
        // VULNERABLE: Displays stored user input without sanitization
        val entriesList = guestbookEntries.map { (name, entry) ->
            GuestbookEntryDto(name = name, entry = entry)
        }

        val response = GuestbookListDto(entries = entriesList)

        return ResponseEntity(response, HttpStatus.OK)
    }
}
