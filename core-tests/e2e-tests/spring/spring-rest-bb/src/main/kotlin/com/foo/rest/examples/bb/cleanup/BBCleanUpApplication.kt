package com.foo.rest.examples.bb.cleanup

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping(path = ["/api/cleanup"])
open class BBCleanUpApplication {

    companion object{

        @JvmStatic
        val data = mutableMapOf<String,BBCleanUpDto>()
    }

    private fun containsUnsafeUrlCharacters(str: String): Boolean {
        // Characters that are problematic in URL path parameters
        val unsafeChars = setOf('<', '>', '"', '\'', '&', '/', '\\', '{', '}', '|', '^', '[', ']', '`', ' ')
        return str.any { it in unsafeChars || it.code < 32 || it.code > 126 }
    }

    @PostMapping(path = ["/items"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun postCreate(@RequestBody dto: BBCleanUpDto) : ResponseEntity<BBCleanUpDto> {

        if(dto.id.isNullOrBlank() || dto.x == null) return ResponseEntity.status(400).build()

        // Check if ID contains characters that are not safe for URL path parameters
        if(containsUnsafeUrlCharacters(dto.id!!)) {
            return ResponseEntity.status(400).build()
        }

        if(data.containsKey(dto.id)){
            return ResponseEntity.status(409).build()
        }

        CoveredTargets.cover("POST")
        data[dto.id!!] = dto
        return ResponseEntity.status(201).body(dto)
    }

    @DeleteMapping(path = ["/items/{id}"])
    fun delete(@PathVariable id: String) : ResponseEntity<Void> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        data.remove(id)
        CoveredTargets.cover("DELETE")
        return ResponseEntity.status(204).build()
    }
}
