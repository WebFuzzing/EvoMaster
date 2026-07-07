package com.foo.rest.examples.bb.jsonpatchdto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/api/jsonpatchdto/resources")
open class BBJsonPatchDtoApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBJsonPatchDtoApplication::class.java, *args)
        }
    }

    private val createdId = 1L
    private val mapper = ObjectMapper()

    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Created")
    ])
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun create(@RequestBody dto: BBJsonPatchResourceDto): ResponseEntity<BBJsonPatchCreatedDto> {
        if (dto.name.isNotBlank())
            CoveredTargets.cover("JSON_PATCH_DTO_CREATED_WITH_NAME")

        CoveredTargets.cover("JSON_PATCH_DTO_CREATED")
        return ResponseEntity.status(HttpStatus.CREATED).body(BBJsonPatchCreatedDto(createdId))
    }

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Patched"),
        ApiResponse(responseCode = "400", description = "Invalid patch document"),
        ApiResponse(responseCode = "404", description = "Missing resource")
    ])
    @PatchMapping("/{id}", consumes = ["application/json-patch+json"], produces = ["text/plain"])
    fun patch(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<String> {
        if (id != createdId)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("missing")

        val patch = parsePatchDocument(body)
        if (patch == null || patch.none { it.path("op").asText() == "replace" })
            return ResponseEntity.badRequest().body("invalid patch")

        CoveredTargets.cover("JSON_PATCH_DTO_PATCHED")
        return ResponseEntity.ok("patched")
    }

    private fun parsePatchDocument(body: String): JsonNode? =
        try {
            mapper.readTree(body).takeIf { it.isArray }
        } catch (e: Exception) {
            null
        }
}

data class BBJsonPatchResourceDto(
    val name: String = "",
    val age: Int = 0
)

data class BBJsonPatchCreatedDto(
    val id: Long = 0
)