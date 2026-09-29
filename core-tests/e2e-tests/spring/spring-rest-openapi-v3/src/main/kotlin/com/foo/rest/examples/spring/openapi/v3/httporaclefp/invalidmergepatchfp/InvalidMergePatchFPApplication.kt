package com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidmergepatchfp

import java.net.URI
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/merge-patch-fp"])
@RestController
open class InvalidMergePatchFPApplication {

    companion object {

        const val MERGE_PATCH = "application/merge-patch+json"

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(InvalidMergePatchFPApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, MergePatchResource>()

        fun reset() {
            data.clear()
        }
    }

    data class MergePatchResource(
        var name: String? = null,
        var value: Int? = null
    )

    class MergeRequest(
        var name: String? = null,
        var value: Int? = null
    )


    @PostMapping
    open fun create(@RequestBody body: MergePatchResource): ResponseEntity<MergePatchResource> {
        val id = data.size + 1
        val stored = body.copy()
        data[id] = stored
        return ResponseEntity.created(URI.create("/api/merge-patch-fp/$id")).body(stored)
    }

    @GetMapping("/{id}")
    open fun get(@PathVariable("id") id: Int): ResponseEntity<MergePatchResource> {
        val resource = data[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PatchMapping("/{id}", consumes = [MERGE_PATCH])
    open fun patch(
        @PathVariable("id") id: Int,
        @RequestBody body: MergeRequest
    ): ResponseEntity<MergePatchResource> {

        val resource = data[id] ?: return ResponseEntity.status(404).build()

        // BUG: overwrites every field unconditionally. A body of {"name":"x"} makes
        // 'value' arrive as null and wipes the stored value -> PATCH acts like PUT.
        resource.name = body.name
        resource.value = body.value

        //but, it returns 202, which means it has not completed yet
        return ResponseEntity.status(202).body(resource)
    }
}
