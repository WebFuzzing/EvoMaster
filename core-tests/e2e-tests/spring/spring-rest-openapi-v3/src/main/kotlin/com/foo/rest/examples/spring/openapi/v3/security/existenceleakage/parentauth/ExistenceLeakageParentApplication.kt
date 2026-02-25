package com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.parentauth

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/api/parents")
open class ExistenceLeakageParentApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ExistenceLeakageParentApplication::class.java, *args)
        }

        private val parents = mutableMapOf<Int, String>()
        private val children = mutableMapOf<Pair<Int, Int>, String>()

        fun reset() {
            parents.clear()
            children.clear()
        }
    }

    private fun checkAuth(auth: String?) =
        auth != null && (auth == "FOO" || auth == "BAR")


    @PutMapping("/{pid}")
    open fun createParent(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable pid: Int
    ): ResponseEntity<Void> {

        if (!checkAuth(auth)) return ResponseEntity.status(401).build()

        if (!parents.containsKey(pid)) {
            parents[pid] = auth!!
            return ResponseEntity.status(201).build()
        }

        return if (parents[pid] != auth)
            ResponseEntity.status(403).build()
        else
            ResponseEntity.status(204).build()
    }

    @GetMapping("/{pid}")
    open fun getParent(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable pid: Int
    ): ResponseEntity<String> {

        if (!checkAuth(auth)) return ResponseEntity.status(401).build()

        if (!parents.containsKey(pid)) {
            return ResponseEntity.status(403).build()
        }

        if (parents[pid] != auth) {
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.ok(parents[pid])
    }

    @PutMapping("/{pid}/children/{cid}")
    open fun createChild(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable pid: Int,
        @PathVariable cid: Int
    ): ResponseEntity<Void> {

        if (!checkAuth(auth)) return ResponseEntity.status(401).build()

        if (!parents.containsKey(pid)) {
            return ResponseEntity.status(404).build()
        }

        if (parents[pid] != auth) {
            return ResponseEntity.status(403).build()
        }

        val key = Pair(pid, cid)
        if (!children.containsKey(key)) {
            children[key] = auth!!
            return ResponseEntity.status(201).build()
        }

        return ResponseEntity.status(204).build()
    }

    @GetMapping("/{pid}/children/{cid}")
    open fun getChild(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable pid: Int,
        @PathVariable cid: Int
    ): ResponseEntity<String> {

        // Authentication is required
        if (!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        // If the parent resource does not exist, returning 403 is correct for everyone
        if (!parents.containsKey(pid)) {
            return ResponseEntity.status(404).build()
        }

        // If the parent exists but is not owned by the caller,
        // we must not leak whether the child exists or not
        if (parents[pid] != auth) {
            return ResponseEntity.status(403).build()
        }

        val key = Pair(pid, cid)

        // At this point, the caller owns the parent.
        // If the child does not exist, returning 404 is legitimate
        if (!children.containsKey(key)) {
            return ResponseEntity.status(404).build()
        }

        // Optional safety check: child ownership
        if (children[key] != auth) {
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.ok(children[key])
    }
}
