package com.foo.rest.examples.spring.openapi.v3.jsonpatch

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
        1L to JsonPatchDto("Doggo", 3),
        2L to JsonPatchDto("Catto", 5)
    )

    @GetMapping("/{id}")
    fun getPet(@PathVariable id: Long): ResponseEntity<JsonPatchDto> {
        val pet = store[id] ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(pet)
    }

    @PatchMapping("/{id}", consumes = ["application/json-patch+json"])
    fun patchPet(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> {
        if (!store.containsKey(id)) return ResponseEntity.notFound().build()
        val trimmed = body.trim()
        if (!trimmed.startsWith("[")) {
            return ResponseEntity.badRequest().body("Patch document must be a JSON array")
        }
        return ResponseEntity.ok("patched")
    }
}

data class JsonPatchDto(val name: String, val age: Int)
