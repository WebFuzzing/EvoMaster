package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidmergepatch

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import java.util.Optional
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * SUT for RFC 7386 (JSON Merge Patch). Two resources under /api/mergepatch:
 *  - /buggy   : PATCH overwrites every field, clobbering ones absent from the body (bug).
 *  - /correct : PATCH only changes fields present in the body.
 */
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
open class InvalidMergePatchApplication {

    companion object {

        // annotation args must be compile-time constants
        const val MERGE_PATCH = "application/merge-patch+json"

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(InvalidMergePatchApplication::class.java, *args)
        }

        private val buggyData = mutableMapOf<Int, ResourceData>()
        private val correctData = mutableMapOf<Int, ResourceData>()

        fun reset() {
            buggyData.clear()
            correctData.clear()
        }
    }

    // stored resource; server-assigned id, name/value nullable so "delete via null" is representable
    data class ResourceData(
        var id: Int? = null,
        var name: String? = null,
        var value: Int? = null
    )

    // POJO bound from a merge-patch body: absent fields deserialize as null,
    // so this cannot tell "absent" from "explicit null" -> root cause of the bug
    class MergeRequest(
        var name: String? = null,
        var value: Int? = null
    )

    // proper merge-patch body: null = absent (untouched), Optional.empty = delete, Optional.of = set
    class MergePatchDto(
        @field:Schema(nullable = true)
        val name: Optional<String>? = null,

        @field:Schema(nullable = true)
        val value: Optional<Int>? = null
    )

    // ----------------------------------------------------------------------
    // buggy resource
    // ----------------------------------------------------------------------

    @PostMapping("/api/mergepatch/buggy")
    open fun createBuggy(@RequestBody body: ResourceData): ResponseEntity<ResourceData> {
        val id = buggyData.size + 1
        val stored = body.copy(id = id)
        buggyData[id] = stored
        return ResponseEntity.created(URI.create("/api/mergepatch/buggy/$id")).body(stored)
    }

    @GetMapping("/api/mergepatch/buggy/{id}")
    open fun getBuggy(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = buggyData[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PatchMapping("/api/mergepatch/buggy/{id}", consumes = [MERGE_PATCH])
    open fun patchBuggy(
        @PathVariable("id") id: Int,
        @RequestBody body: MergeRequest
    ): ResponseEntity<ResourceData> {

        val resource = buggyData[id] ?: return ResponseEntity.status(404).build()

        // BUG: overwrites every field unconditionally. A body of {"name":"x"} makes
        // 'value' arrive as null and wipes the stored value -> PATCH acts like PUT.
        resource.name = body.name
        resource.value = body.value

        return ResponseEntity.status(200).body(resource)
    }

    // ----------------------------------------------------------------------
    // correct resource
    // ----------------------------------------------------------------------

    @PostMapping("/api/mergepatch/correct")
    open fun createCorrect(@RequestBody body: ResourceData): ResponseEntity<ResourceData> {
        val id = correctData.size + 1
        val stored = body.copy(id = id)
        correctData[id] = stored
        return ResponseEntity.created(URI.create("/api/mergepatch/correct/$id")).body(stored)
    }

    @GetMapping("/api/mergepatch/correct/{id}")
    open fun getCorrect(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = correctData[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PatchMapping("/api/mergepatch/correct/{id}", consumes = [MERGE_PATCH])
    open fun patchCorrect(
        @PathVariable("id") id: Int,
        @RequestBody body: MergePatchDto
    ): ResponseEntity<ResourceData> {

        val resource = correctData[id] ?: return ResponseEntity.status(404).build()

        // RFC 7386: absent (null property) -> untouched; Optional.empty -> delete;
        // Optional.of(v) -> set. Only fields actually present in the body are applied.
        body.name?.let { resource.name = it.orElse(null) }
        body.value?.let { resource.value = it.orElse(null) }

        return ResponseEntity.status(200).body(resource)
    }
}
