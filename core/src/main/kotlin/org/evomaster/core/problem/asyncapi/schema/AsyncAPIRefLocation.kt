package org.evomaster.core.problem.asyncapi.schema

import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import java.net.URI
import java.security.MessageDigest

/**
 * URI / fragment helpers for AsyncAPI external `$ref` resolution.
 *
 * AsyncAPI uses the same `$ref` semantics as OpenAPI; the resolution rules here
 * mirror `org.evomaster.core.problem.rest.schema.SchemaUtils` so the two
 * protocols agree on how a relative ref like `./shared-types.yaml#/components/schemas/Order`
 * resolves against a primary document's location. Kept AsyncAPI-local rather
 * than shared up to `search/gene/builder/` to keep this PR's blast radius
 * narrow; REST's copy is still authoritative for OpenAPI.
 */
internal object AsyncAPIRefLocation {

    private const val FRAGMENT_DELIMITER = "#"

    /** True if the ref is purely intra-document (starts with `#`). */
    fun isLocalRef(ref: String): Boolean = ref.startsWith(FRAGMENT_DELIMITER)

    /** Split `<location>#<fragment>` into a pair. Either side may be empty. */
    fun split(ref: String): Pair<String, String> {
        val idx = ref.indexOf(FRAGMENT_DELIMITER)
        if (idx < 0) return ref to ""
        return ref.substring(0, idx) to ref.substring(idx + 1)
    }

    /**
     * Resolve a possibly-relative external location against the primary
     * document's location. Mirrors `SchemaUtils.computeLocation`.
     *
     * Returns the absolute resolved location string (URL or filesystem path).
     */
    fun resolveAbsolute(rawLocation: String, currentSource: SchemaLocation): String {
        if (rawLocation.isBlank()) {
            // Self-reference (fragment-only) is handled by the caller; we only
            // get here when the caller already split off a non-empty location.
            return currentSource.location
        }

        if (rawLocation.startsWith("http:", ignoreCase = true) ||
            rawLocation.startsWith("https:", ignoreCase = true) ||
            rawLocation.startsWith("file:", ignoreCase = true)
        ) {
            return rawLocation
        }

        if (currentSource.type == SchemaLocationType.MEMORY) {
            throw IllegalArgumentException(
                "Cannot resolve relative AsyncAPI ref '$rawLocation' against an in-memory source"
            )
        }

        val csl = currentSource.location

        if (rawLocation.startsWith("//")) {
            val protocol = csl.substring(0, csl.indexOf(":").coerceAtLeast(0))
            return if (protocol.isNotBlank()) "$protocol:$rawLocation" else rawLocation
        }

        return try {
            // Treat the source location as a base URI, then resolve the relative
            // path. Falls back to string concatenation when the source is not a
            // valid URI (e.g. bare filesystem path).
            URI(csl).resolve(rawLocation).normalize().toString()
        } catch (_: Exception) {
            val baseDir = csl.substringBeforeLast('/', missingDelimiterValue = "")
            if (baseDir.isBlank()) rawLocation else "$baseDir/$rawLocation"
        }
    }

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
