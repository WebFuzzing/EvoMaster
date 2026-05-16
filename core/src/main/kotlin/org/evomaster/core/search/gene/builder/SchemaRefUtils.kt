package org.evomaster.core.search.gene.builder

import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Cross-protocol `$ref` location helpers.
 *
 * OpenAPI (REST) and AsyncAPI use the same `$ref` semantics — a string of the
 * form `<location>#<fragment>` where `<location>` may be absolute, relative,
 * or empty (intra-document). This object owns the parsing and resolution
 * rules so both protocols agree on how a ref like
 * `./shared-types.yaml#/components/schemas/Order` resolves against a primary
 * document's source location.
 *
 * History: previously split between
 * `org.evomaster.core.problem.rest.schema.SchemaUtils` (REST-side) and
 * `org.evomaster.core.problem.asyncapi.schema.AsyncAPIRefLocation`
 * (AsyncAPI-side); the two implementations were behaviour-equivalent on
 * common inputs but neither was canonical. Consolidated here in M9-PR1.
 *
 * Two calling idioms are exposed:
 *
 *  - REST-style `(*, messages: MutableList<String>): T?` — null return with
 *    diagnostics in the message list. Used by REST's `SchemaUtils` chain
 *    where errors are accumulated and reported in bulk.
 *  - AsyncAPI-style direct functions ([splitRef], [resolveRawLocation]) —
 *    throw on errors. Used by `AsyncAPIAccess.inlineExternalRefs` which
 *    fails fast on the first malformed ref.
 *
 * Both idioms share the same algorithm; pick whichever fits the caller.
 */
object SchemaRefUtils {

    private val log = LoggerFactory.getLogger(SchemaRefUtils::class.java)

    /** True if [ref] is a purely intra-document reference (starts with `#`). */
    fun isLocalRef(ref: String): Boolean = ref.startsWith("#")

    /**
     * Split a `$ref` string into its `(location, fragment)` parts. The
     * fragment is returned WITHOUT the leading `#`. Either side may be empty:
     *  - `#/components/schemas/X`     → `("", "/components/schemas/X")`
     *  - `./shared.yaml#/.../Order`   → `("./shared.yaml", "/.../Order")`
     *  - `./shared.yaml`              → `("./shared.yaml", "")`
     *
     * AsyncAPI-style: never returns null; callers that want REST's "no `#`
     * is an error" semantics should use [extractLocation] instead.
     */
    fun splitRef(ref: String): Pair<String, String> {
        val hash = ref.indexOf("#")
        if (hash < 0) return ref to ""
        return ref.substring(0, hash) to ref.substring(hash + 1)
    }

    /**
     * REST-style extraction of the location portion of [sref]. Returns null
     * and appends a diagnostic to [messages] if the ref contains no `#`.
     * Kept for compatibility with REST's diagnostics chain; AsyncAPI uses
     * [splitRef] directly.
     */
    fun extractLocation(sref: String, messages: MutableList<String>): String? {
        if (!sref.contains("#")) {
            messages.add("Not a valid \$ref, as it contains no #: $sref")
            return null
        }
        return sref.substring(0, sref.indexOf("#"))
    }

    /**
     * REST-style: resolve the location portion of [ref] against [currentSource].
     * Returns null with a diagnostic if extraction fails; throws
     * [IllegalArgumentException] for relative refs against an in-memory source.
     */
    fun computeLocation(
        ref: String,
        currentSource: SchemaLocation,
        messages: MutableList<String>
    ): String? {
        val rawLocation = extractLocation(ref, messages) ?: return null
        return resolveRawLocation(rawLocation, currentSource)
    }

    /**
     * AsyncAPI-style: resolve a bare [rawLocation] (no fragment) against
     * [currentSource]. Returns the absolute resolved location string.
     *
     * Throws [IllegalArgumentException] when the source is in-memory and
     * the location is relative.
     */
    fun resolveRawLocation(rawLocation: String, currentSource: SchemaLocation): String {
        if (rawLocation.startsWith("http:", ignoreCase = true) ||
            rawLocation.startsWith("https:", ignoreCase = true) ||
            rawLocation.startsWith("file:", ignoreCase = true)
        ) {
            // location is absolute, so no need to do anything
            return rawLocation
        }

        if (currentSource.type == SchemaLocationType.MEMORY) {
            throw IllegalArgumentException("Can't handle relative location for memory files: $rawLocation")
        }

        val csl = currentSource.location

        if (rawLocation.startsWith("//")) {
            // per the OpenAPI/AsyncAPI specs, use the same protocol as source
            val protocol = csl.substring(0, csl.indexOf(":").coerceAtLeast(0))
            if (protocol.isBlank()) {
                log.warn("No protocol can be inferred for $rawLocation from $csl")
            }
            return "$protocol:$rawLocation"
        }

        // Relative path. The "$csl/../$rawLocation" + URI.normalize() approach
        // was chosen by REST years ago to dodge subtle differences in Java's
        // URI.resolve behaviour across JDK versions; keeping it here preserves
        // REST's existing test coverage and produces the same results for
        // every input AsyncAPI exercises.
        val delimiter = if (csl.endsWith("/")) "" else "/"
        val parentFolder = "../"
        val location = "$csl$delimiter$parentFolder$rawLocation"

        return try {
            URI(location).normalize().toString()
        } catch (_: Exception) {
            location
        }
    }
}
