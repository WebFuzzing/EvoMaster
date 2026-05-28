package com.foo.rest.examples.bb.jsonpatch

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
        1L to BBJsonPatchDto("Doggo", 3),
        2L to BBJsonPatchDto("Catto", 5)
    )

    @GetMapping("/{id}")
    fun getPet(@PathVariable id: Long): ResponseEntity<BBJsonPatchDto> {
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
        CoveredTargets.cover("PATCHED")
        return ResponseEntity.ok("patched")
    }
}

data class BBJsonPatchDto(val name: String, val age: Int)
