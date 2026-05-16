package org.evomaster.core.problem.asyncapi.schema

import com.sun.net.httpserver.HttpServer
import org.evomaster.core.remote.SutProblemException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path

/**
 * Covers the external `$ref` resolution pre-pass added in M9-PR1.
 *
 * Three resolution paths are exercised:
 *   - sibling file in the same directory
 *   - sub-folder reference (relative path with one or more segments)
 *   - HTTP-served document (stubbed via `com.sun.net.httpserver`)
 *
 * Two negative paths:
 *   - cross-file reference to an unsupported component category fails loudly
 *   - chained external refs (external A → external B) resolve transitively
 */
class AsyncAPIAccessExternalRefsTest {

    // -----------------------------------------------------------------------
    // Sibling file: ./shared-types.yaml#/components/schemas/RequestPayload
    // -----------------------------------------------------------------------
    @Test
    fun resolveSiblingFileRef(@TempDir tempDir: Path) {
        val sibling = tempDir.resolve("shared-types.yaml")
        Files.writeString(
            sibling,
            """
            components:
              schemas:
                RequestPayload:
                  type: object
                  properties:
                    name: { type: string }
                  required: [name]
            """.trimIndent()
        )

        val primary = tempDir.resolve("api.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M:
                  name: M
                  payload:
                    ${'$'}ref: './shared-types.yaml#/components/schemas/RequestPayload'
            """.trimIndent()
        )

        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())

        // The synthetic key for the inlined external schema is namespaced.
        val schemaKey = schema.componentSchemas.keys.singleOrNull { it.endsWith("_RequestPayload") }
        assertNotNull(schemaKey, "expected one inlined external schema; got ${schema.componentSchemas.keys}")
        assertTrue(schemaKey!!.startsWith("_ext_"), "unexpected synthetic key: $schemaKey")

        // The message's payloadSchemaRef should now point at the namespaced key.
        val message = schema.messages["M"]!!
        assertEquals(schemaKey, message.payloadSchemaRef)
        assertNull(message.payloadInline)
    }

    // -----------------------------------------------------------------------
    // Sub-folder: ./schemas/types.yaml#/components/schemas/Order
    // -----------------------------------------------------------------------
    @Test
    fun resolveSubFolderRef(@TempDir tempDir: Path) {
        val schemasDir = tempDir.resolve("schemas")
        Files.createDirectories(schemasDir)
        Files.writeString(
            schemasDir.resolve("types.yaml"),
            """
            components:
              schemas:
                Order:
                  type: object
                  properties:
                    id: { type: string }
            """.trimIndent()
        )

        val primary = tempDir.resolve("api.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M:
                  name: M
                  payload:
                    ${'$'}ref: './schemas/types.yaml#/components/schemas/Order'
            """.trimIndent()
        )

        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())
        val key = schema.componentSchemas.keys.singleOrNull { it.endsWith("_Order") }
        assertNotNull(key)
    }

    // -----------------------------------------------------------------------
    // HTTP: refers a document served by an in-test HTTP server.
    // -----------------------------------------------------------------------
    @Test
    fun resolveHttpRef() {
        val externalBody = """
            components:
              schemas:
                RemoteEvent:
                  type: object
                  properties:
                    eventId: { type: string }
        """.trimIndent()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/types.yaml") { exchange ->
            val bytes = externalBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/yaml")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val port = server.address.port

        try {
            val primaryText = """
                asyncapi: 3.0.0
                info: { title: t, version: '1' }
                channels:
                  c1:
                    address: topic.a
                    messages:
                      M: { ${'$'}ref: '#/components/messages/M' }
                operations:
                  op1:
                    action: send
                    channel: { ${'$'}ref: '#/channels/c1' }
                components:
                  messages:
                    M:
                      name: M
                      payload:
                        ${'$'}ref: 'http://127.0.0.1:$port/types.yaml#/components/schemas/RemoteEvent'
            """.trimIndent()

            val schema = AsyncAPIAccess.parse(
                primaryText,
                org.evomaster.core.problem.rest.schema.SchemaLocation(
                    "http://127.0.0.1:$port/primary.yaml",
                    org.evomaster.core.problem.rest.schema.SchemaLocationType.REMOTE
                )
            )

            val key = schema.componentSchemas.keys.singleOrNull { it.endsWith("_RemoteEvent") }
            assertNotNull(key, "expected one inlined remote schema; got ${schema.componentSchemas.keys}")
        } finally {
            server.stop(0)
        }
    }

    // -----------------------------------------------------------------------
    // Chained external refs: A.yaml refs B.yaml. Both must be loaded and
    // their internal refs prefixed with their own namespace.
    // -----------------------------------------------------------------------
    @Test
    fun resolveChainedExternalRefs(@TempDir tempDir: Path) {
        Files.writeString(
            tempDir.resolve("b.yaml"),
            """
            components:
              schemas:
                Inner:
                  type: object
                  properties:
                    value: { type: integer }
            """.trimIndent()
        )

        Files.writeString(
            tempDir.resolve("a.yaml"),
            """
            components:
              schemas:
                Outer:
                  type: object
                  properties:
                    inner:
                      ${'$'}ref: './b.yaml#/components/schemas/Inner'
            """.trimIndent()
        )

        val primary = tempDir.resolve("primary.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M:
                  name: M
                  payload:
                    ${'$'}ref: './a.yaml#/components/schemas/Outer'
            """.trimIndent()
        )

        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())

        // Both Outer and Inner should be inlined with their own namespaces.
        val outerKey = schema.componentSchemas.keys.singleOrNull { it.endsWith("_Outer") }
        val innerKey = schema.componentSchemas.keys.singleOrNull { it.endsWith("_Inner") }
        assertNotNull(outerKey, "Outer not inlined; got keys=${schema.componentSchemas.keys}")
        assertNotNull(innerKey, "Inner not inlined; got keys=${schema.componentSchemas.keys}")

        // Inside Outer, the inner ref should point at Inner's namespaced key.
        val outerNode = schema.componentSchemas[outerKey]!!
        val innerRef = outerNode.get("properties")?.get("inner")?.get("\$ref")?.asText()
        assertEquals("#/components/schemas/$innerKey", innerRef)
    }

    // -----------------------------------------------------------------------
    // Unsupported category: cross-file parameter refs aren't inlined.
    // -----------------------------------------------------------------------
    @Test
    fun rejectUnsupportedCrossFileCategory(@TempDir tempDir: Path) {
        Files.writeString(
            tempDir.resolve("other.yaml"),
            """
            channels:
              other:
                address: topic.other
            """.trimIndent()
        )

        val primary = tempDir.resolve("primary.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
              external:
                ${'$'}ref: './other.yaml#/channels/other'
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M: { name: M, payload: { type: object } }
            """.trimIndent()
        )

        val ex = assertThrows(SutProblemException::class.java) {
            AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())
        }
        assertTrue(
            ex.message!!.contains("not supported"),
            "expected explicit error about cross-file channel refs; got: ${ex.message}"
        )
    }

    // -----------------------------------------------------------------------
    // Circular external refs: A.yaml's schema references B.yaml's, and
    // B.yaml's schema references A.yaml's right back. The load-phase dedup
    // (`if (absoluteLoc in externalDocs) continue`) MUST break the cycle,
    // otherwise the parser would loop forever.
    // -----------------------------------------------------------------------
    @Test
    fun circularExternalRefsTerminate(@TempDir tempDir: Path) {
        Files.writeString(
            tempDir.resolve("a.yaml"),
            """
            components:
              schemas:
                AlphaNode:
                  type: object
                  properties:
                    next:
                      ${'$'}ref: './b.yaml#/components/schemas/BetaNode'
            """.trimIndent()
        )
        Files.writeString(
            tempDir.resolve("b.yaml"),
            """
            components:
              schemas:
                BetaNode:
                  type: object
                  properties:
                    back:
                      ${'$'}ref: './a.yaml#/components/schemas/AlphaNode'
            """.trimIndent()
        )

        val primary = tempDir.resolve("primary.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M:
                  name: M
                  payload:
                    ${'$'}ref: './a.yaml#/components/schemas/AlphaNode'
            """.trimIndent()
        )

        // The test passes if this returns within the JUnit default timeout
        // (i.e., the parser terminates rather than looping). Belt-and-braces:
        // also check that both schemas were loaded and their cross-doc refs
        // were rewritten to namespaced local refs.
        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())
        val alphaKey = schema.componentSchemas.keys.singleOrNull { it.endsWith("_AlphaNode") }
        val betaKey = schema.componentSchemas.keys.singleOrNull { it.endsWith("_BetaNode") }
        assertNotNull(alphaKey, "AlphaNode not inlined; got ${schema.componentSchemas.keys}")
        assertNotNull(betaKey, "BetaNode not inlined; got ${schema.componentSchemas.keys}")

        // AlphaNode.next.$ref → namespaced BetaNode
        val nextRef = schema.componentSchemas[alphaKey]!!
            .get("properties")?.get("next")?.get("\$ref")?.asText()
        assertEquals("#/components/schemas/$betaKey", nextRef)

        // BetaNode.back.$ref → namespaced AlphaNode (the cycle is closed by
        // intra-doc refs after the load phase resolves both external sources).
        val backRef = schema.componentSchemas[betaKey]!!
            .get("properties")?.get("back")?.get("\$ref")?.asText()
        assertEquals("#/components/schemas/$alphaKey", backRef)
    }

    // -----------------------------------------------------------------------
    // Back-reference to the primary doc: an external doc has a $ref pointing
    // *back* at the primary. The parser must not re-load+duplicate the
    // primary's own components; the ref should rewrite to a plain local one.
    // -----------------------------------------------------------------------
    @Test
    fun externalDocBackReferenceToPrimaryRewritesAsLocal(@TempDir tempDir: Path) {
        Files.writeString(
            tempDir.resolve("a.yaml"),
            """
            components:
              schemas:
                Wrapper:
                  type: object
                  properties:
                    inner:
                      ${'$'}ref: './primary.yaml#/components/schemas/PrimaryOnly'
            """.trimIndent()
        )

        val primary = tempDir.resolve("primary.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M:
                  name: M
                  payload:
                    ${'$'}ref: './a.yaml#/components/schemas/Wrapper'
              schemas:
                PrimaryOnly:
                  type: object
                  properties:
                    answer: { type: integer }
            """.trimIndent()
        )

        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())

        // Wrapper got inlined under a namespaced key.
        val wrapperKey = schema.componentSchemas.keys.singleOrNull { it.endsWith("_Wrapper") }
        assertNotNull(wrapperKey, "Wrapper not inlined; got ${schema.componentSchemas.keys}")

        // PrimaryOnly stayed at its primary-doc key (no namespacing).
        assertNotNull(
            schema.componentSchemas["PrimaryOnly"],
            "primary's own PrimaryOnly schema should still exist under its original key"
        )

        // The back-reference from Wrapper now resolves to PrimaryOnly's
        // local key (not an `_ext_<...>_PrimaryOnly` duplicate).
        val innerRef = schema.componentSchemas[wrapperKey]!!
            .get("properties")?.get("inner")?.get("\$ref")?.asText()
        assertEquals("#/components/schemas/PrimaryOnly", innerRef)

        // And no namespaced duplicate of PrimaryOnly was inlined.
        assertTrue(
            schema.componentSchemas.keys.none { it.endsWith("_PrimaryOnly") },
            "primary doc shouldn't be inlined as a namespaced duplicate of itself; got ${schema.componentSchemas.keys}"
        )
    }

    // -----------------------------------------------------------------------
    // Missing external file: clear error referencing both ref and source.
    // -----------------------------------------------------------------------
    @Test
    fun missingExternalFileFailsClearly(@TempDir tempDir: Path) {
        val primary = tempDir.resolve("primary.yaml")
        Files.writeString(
            primary,
            """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c1:
                address: topic.a
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op1:
                action: send
                channel: { ${'$'}ref: '#/channels/c1' }
            components:
              messages:
                M:
                  name: M
                  payload:
                    ${'$'}ref: './does-not-exist.yaml#/components/schemas/X'
            """.trimIndent()
        )

        val ex = assertThrows(SutProblemException::class.java) {
            AsyncAPIAccess.getAsyncAPIFromLocation(primary.toString())
        }
        assertTrue(
            ex.message!!.contains("does-not-exist.yaml"),
            "expected error to mention the missing file path; got: ${ex.message}"
        )
    }
}
