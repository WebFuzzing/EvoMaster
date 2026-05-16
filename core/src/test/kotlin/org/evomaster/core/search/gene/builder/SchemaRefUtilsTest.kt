package org.evomaster.core.search.gene.builder

import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function coverage of the shared `$ref` location helpers.
 *
 * Behaviour is mirrored from REST's previous `SchemaUtils` implementation
 * and the AsyncAPI-side `AsyncAPIRefLocation` — neither was canonical before
 * M9-PR1. These tests lock the consolidated rules so future protocol
 * additions can rely on them.
 */
class SchemaRefUtilsTest {

    @Test
    fun isLocalRefMatchesLeadingHash() {
        assertTrue(SchemaRefUtils.isLocalRef("#/components/schemas/X"))
        assertTrue(SchemaRefUtils.isLocalRef("#"))
        assertEquals(false, SchemaRefUtils.isLocalRef("./shared.yaml#/...") )
        assertEquals(false, SchemaRefUtils.isLocalRef("http://example/.../X"))
        assertEquals(false, SchemaRefUtils.isLocalRef(""))
    }

    @Test
    fun splitRefDecomposesLocationAndFragment() {
        assertEquals("./shared.yaml" to "/components/schemas/Order",
            SchemaRefUtils.splitRef("./shared.yaml#/components/schemas/Order"))
        assertEquals("" to "/components/schemas/X",
            SchemaRefUtils.splitRef("#/components/schemas/X"))
        assertEquals("./shared.yaml" to "",
            SchemaRefUtils.splitRef("./shared.yaml"))
        assertEquals("" to "",
            SchemaRefUtils.splitRef(""))
    }

    @Test
    fun extractLocationReturnsNullAndAppendsMessageWhenNoHash() {
        val messages = mutableListOf<String>()
        assertNull(SchemaRefUtils.extractLocation("no-hash-here.yaml", messages))
        assertTrue(messages.single().contains("no #"), "expected diagnostic; got $messages")
    }

    @Test
    fun extractLocationReturnsLocationWhenHashPresent() {
        val messages = mutableListOf<String>()
        assertEquals("./shared.yaml",
            SchemaRefUtils.extractLocation("./shared.yaml#/components/schemas/X", messages))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun computeLocationHandlesAbsoluteUrls() {
        val messages = mutableListOf<String>()
        val primary = SchemaLocation("file:/primary.yaml", SchemaLocationType.LOCAL)
        val resolved = SchemaRefUtils.computeLocation(
            "http://example.com/api/types.yaml#/components/schemas/X",
            primary, messages
        )
        assertEquals("http://example.com/api/types.yaml", resolved)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun computeLocationResolvesRelativePathsAgainstFileSource() {
        val messages = mutableListOf<String>()
        val primary = SchemaLocation("/tmp/x/primary.yaml", SchemaLocationType.LOCAL)
        val resolved = SchemaRefUtils.computeLocation(
            "./shared.yaml#/components/schemas/Order",
            primary, messages
        )
        assertEquals("/tmp/x/shared.yaml", resolved)
    }

    @Test
    fun computeLocationResolvesSubFolderRelativePaths() {
        val messages = mutableListOf<String>()
        val primary = SchemaLocation("/tmp/x/primary.yaml", SchemaLocationType.LOCAL)
        val resolved = SchemaRefUtils.computeLocation(
            "schemas/types.yaml#/components/schemas/Order",
            primary, messages
        )
        assertEquals("/tmp/x/schemas/types.yaml", resolved)
    }

    @Test
    fun computeLocationResolvesAgainstRemoteHttpSource() {
        val messages = mutableListOf<String>()
        val primary = SchemaLocation("http://example.com/api/primary.yaml", SchemaLocationType.REMOTE)
        val resolved = SchemaRefUtils.computeLocation(
            "./shared.yaml#/components/schemas/X",
            primary, messages
        )
        assertEquals("http://example.com/api/shared.yaml", resolved)
    }

    @Test
    fun computeLocationPropagatesProtocolForProtocolRelative() {
        val messages = mutableListOf<String>()
        val primary = SchemaLocation("https://example.com/api/primary.yaml", SchemaLocationType.REMOTE)
        val resolved = SchemaRefUtils.computeLocation(
            "//other.example.com/x.yaml#/components/schemas/X",
            primary, messages
        )
        assertEquals("https://other.example.com/x.yaml", resolved)
    }

    @Test
    fun resolveRawLocationThrowsForRelativeRefAgainstMemorySource() {
        val memory = SchemaLocation.MEMORY
        assertThrows(IllegalArgumentException::class.java) {
            SchemaRefUtils.resolveRawLocation("./shared.yaml", memory)
        }
    }

    @Test
    fun resolveRawLocationPassesThroughAbsoluteRefsAgainstMemorySource() {
        // file:/, http: and https: are absolute — no path resolution needed,
        // so even an in-memory source is acceptable.
        val memory = SchemaLocation.MEMORY
        assertEquals("http://example/x.yaml",
            SchemaRefUtils.resolveRawLocation("http://example/x.yaml", memory))
        assertEquals("file:/abs/x.yaml",
            SchemaRefUtils.resolveRawLocation("file:/abs/x.yaml", memory))
    }
}
