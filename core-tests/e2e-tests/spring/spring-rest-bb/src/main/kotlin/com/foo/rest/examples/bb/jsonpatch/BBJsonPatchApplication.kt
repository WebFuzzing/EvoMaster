package com.foo.rest.examples.bb.jsonpatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/pets")
open class BBJsonPatchApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBJsonPatchApplication::class.java, *args)
        }
    }

    private val store: MutableMap<Long, BBJsonPatchDto> = mutableMapOf(
        1L to BBJsonPatchDto("Dog", 3),
        2L to BBJsonPatchDto("Cat", 5)
    )

    private val mapper = ObjectMapper()

    @GetMapping("/{id}")
    fun getPet(@PathVariable id: Long): ResponseEntity<BBJsonPatchDto> {
        val pet = store[id] ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(pet)
    }

    @PatchMapping("/{id}", consumes = ["application/json-patch+json"])
    fun patchPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> {
        if (parsePatchDocument(body) == null)
            return ResponseEntity.badRequest().body("Patch document must be a JSON array")

        CoveredTargets.cover("PATCHED")
        return ResponseEntity.ok("patched")
    }

    @PatchMapping("/{id}/add", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun addPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(body, "add", "JSON_PATCH_ADD")

    @PatchMapping("/{id}/remove", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun removePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(body, "remove", "JSON_PATCH_REMOVE")

    @PatchMapping("/{id}/replace", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun replacePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(body, "replace", "JSON_PATCH_REPLACE")

    @PatchMapping("/{id}/move", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun movePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(body, "move", "JSON_PATCH_MOVE")

    @PatchMapping("/{id}/copy", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun copyPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(body, "copy", "JSON_PATCH_COPY")

    @PatchMapping("/{id}/test", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun testPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> =
        patchOperation(body, "test", "JSON_PATCH_TEST")

    @PatchMapping("/{id}/sequence", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun sequencePet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> {
        if (!hasMultipleOperations(body))
            return ResponseEntity.badRequest().body("Patch document must contain at least two operations")

        CoveredTargets.cover("JSON_PATCH_SEQUENCE")
        return ResponseEntity.ok("sequence patched")
    }

    private fun patchOperation(body: String, operation: String, target: String): ResponseEntity<String> {
        if (!hasOperation(body, operation))
            return ResponseEntity.badRequest().body("Patch document must contain a $operation operation")

        CoveredTargets.cover(target)
        return ResponseEntity.ok("$operation patched")
    }

    private fun hasOperation(body: String, operation: String): Boolean =
        parsePatchDocument(body)?.any { it.path("op").asText() == operation } ?: false

    private fun hasMultipleOperations(body: String): Boolean =
        parsePatchDocument(body)
            ?.takeIf { it.size() >= 2 }
            ?.all { it.hasNonNull("op") }
            ?: false

    private fun parsePatchDocument(body: String): JsonNode? =
        try {
            mapper.readTree(body).takeIf { it.isArray }
        } catch (e: Exception) {
            null
        }
}

data class BBJsonPatchDto(val name: String = "", val age: Int = 0)