package com.foo.rest.examples.spring.openapi.v3.jsonpatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/pets")
open class JsonPatchApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(JsonPatchApplication::class.java, *args)
        }
    }

    private val store: MutableMap<Long, JsonPatchDto> = mutableMapOf(
        1L to JsonPatchDto("Dog", 3),
        2L to JsonPatchDto("Cat", 5)
    )

    private val mapper = ObjectMapper()

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Pet found"),
        ApiResponse(responseCode = "400", description = "Invalid pet id")
    ])
    @GetMapping("/{id}")
    fun getPet(@PathVariable id: Long): ResponseEntity<JsonPatchDto> {
        val pet = store[id] ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(pet)
    }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Patch document processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is invalid or patch document is not a JSON array")
    ])
    @PatchMapping("/{id}", consumes = ["application/json-patch+json"])
    fun patchPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> {
        if (!store.containsKey(id)) return ResponseEntity.badRequest().body("Invalid pet id")

        if (parsePatchDocument(body) == null)
            return ResponseEntity.badRequest().body("Patch document must be a JSON array")

        return ResponseEntity.ok("patched")
    }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Add operation processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not contain an add operation for /a")
    ])
    @PatchMapping("/{id}/add", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun addPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(id, body, "add") { it.path("path").asText() == "/a" }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Remove operation processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not contain a remove operation for /b")
    ])
    @PatchMapping("/{id}/remove", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun removePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(id, body, "remove") { it.path("path").asText() == "/b" }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Replace operation processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not contain a replace operation for /c")
    ])
    @PatchMapping("/{id}/replace", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun replacePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(id, body, "replace") { it.path("path").asText() == "/c" }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Move operation processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not contain a move operation from /a to /d")
    ])
    @PatchMapping("/{id}/move", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun movePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(id, body, "move") {
            it.path("from").asText() == "/a" && it.path("path").asText() == "/d"
        }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Copy operation processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not contain a copy operation from /a to /d")
    ])
    @PatchMapping("/{id}/copy", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun copyPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(id, body, "copy") {
            it.path("from").asText() == "/a" && it.path("path").asText() == "/d"
        }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Test operation processed successfully"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not contain a test operation for /b")
    ])
    @PatchMapping("/{id}/test", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun testPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(id, body, "test") { it.path("path").asText() == "/b" }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Patch document has multiple operations"),
        ApiResponse(responseCode = "400", description = "Pet id is not positive or patch document does not add /a before replacing /c")
    ])
    @PatchMapping("/{id}/sequence", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun sequencePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> {
        if (id <= 0)
            return ResponseEntity.badRequest().body("Pet id must be positive")

        if (!hasAddThenReplaceSequence(body))
            return ResponseEntity.badRequest().body("Patch document must contain add followed by replace")

        return ResponseEntity.ok("sequence patched")
    }

    private fun patchOperation(
        id: Long,
        body: String,
        operation: String,
        extraCheck: (JsonNode) -> Boolean
    ): ResponseEntity<String> {
        if (id <= 0)
            return ResponseEntity.badRequest().body("Pet id must be positive")

        if (!hasOperation(body, operation, extraCheck))
            return ResponseEntity.badRequest().body("Patch document must contain a $operation operation")

        return ResponseEntity.ok("$operation patched")
    }

    private fun hasOperation(body: String, operation: String, extraCheck: (JsonNode) -> Boolean): Boolean =
        parsePatchDocument(body)?.any { it.path("op").asText() == operation && extraCheck(it) } ?: false

    private fun hasAddThenReplaceSequence(body: String): Boolean {
        val operations = parsePatchDocument(body) ?: return false
        if (operations.size() < 2)
            return false

        var hasAdd = false
        var hasReplace = false

        for (operation in operations) {
            if (operation.path("op").asText() == "add" && operation.path("path").asText() == "/a")
                hasAdd = true
            if (operation.path("op").asText() == "replace" && operation.path("path").asText() == "/c")
                hasReplace = true
        }

        return hasAdd && hasReplace
    }

    private fun parsePatchDocument(body: String): JsonNode? =
        try {
            mapper.readTree(body).takeIf { it.isArray }
        } catch (e: Exception) {
            null
        }
}

data class JsonPatchDto(val name: String = "", val age: Int = 0)