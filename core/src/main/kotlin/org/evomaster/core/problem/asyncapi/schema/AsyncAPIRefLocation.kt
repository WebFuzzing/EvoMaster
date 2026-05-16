package org.evomaster.core.problem.asyncapi.schema

import java.security.MessageDigest

/**
 * AsyncAPI-specific `$ref` helpers that aren't shared with REST.
 *
 * The URI / location parsing and resolution rules — `isLocalRef`, `splitRef`,
 * and the relative-path resolver — live in
 * `org.evomaster.core.search.gene.builder.SchemaRefUtils` (shared with REST).
 * What stays AsyncAPI-local is the synthetic-key generation used when
 * inlining components from external sources into the primary document.
 * REST doesn't need it because `swagger-parser` handles the inlining
 * for OpenAPI.
 */
internal object AsyncAPIRefLocation {

    /**
     * Short, deterministic key prefix for a resolved external document. Used
     * to namespace components inlined from external sources so they cannot
     * collide with primary-document components of the same name.
     *
     * Example: `_ext_<8 hex>_<safe name>` — the hash makes it deterministic
     * for the same external source across runs (helpful when generated tests
     * are diffed).
     */
    fun externalKeyPrefix(absoluteLocation: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(absoluteLocation.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "_ext_${hex.substring(0, 8)}_"
    }
}
