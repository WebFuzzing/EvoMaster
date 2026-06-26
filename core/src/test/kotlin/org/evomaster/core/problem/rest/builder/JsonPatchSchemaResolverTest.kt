package org.evomaster.core.problem.rest.builder

import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchSchemaResolverTest {

    companion object {
        private val schema: RestSchema by lazy {
            val json = JsonPatchSchemaResolverTest::class.java
                .getResourceAsStream("/swagger/artificial/jsonpatch/json-patch-schema-resolver.json")!!
                .bufferedReader().readText()
            RestSchema(OpenApiAccess.parseOpenApi(json, SchemaLocation.MEMORY))
        }
    }

    private fun resolveForPatch(
        path: String,
        messages: MutableList<String> = mutableListOf()
    ) = JsonPatchSchemaResolver.resolveResourceSchema(
        schema.main.schemaParsed.paths[path]!!.patch,
        schema,
        schema.main,
        messages
    )

    @Test
    fun testResolveFromGetResponse() {
        val messages = mutableListOf<String>()

        val result = resolveForPatch("/pets/{id}", messages)

        assertNotNull(result)
        assertTrue(messages.isEmpty(), "Unexpected messages: $messages")
        val props = result!!.properties
        assertNotNull(props)
        assertTrue(props.containsKey("name"), "Expected property 'name'")
        assertTrue(props.containsKey("age"), "Expected property 'age'")
    }

    @Test
    fun testPreferGetOverPut() {
        val result = resolveForPatch("/x/{id}")

        assertNotNull(result)
        val props = result!!.properties
        assertTrue(props.containsKey("fromGet"), "Should prefer GET schema, got $props")
        assertFalse(props.containsKey("fromPut"))
    }

    @Test
    fun testFallbackToPut() {
        val result = resolveForPatch("/orders/{id}")

        assertNotNull(result)
        val props = result!!.properties
        assertTrue(props.containsKey("product"), "Expected 'product' in $props")
        assertTrue(props.containsKey("quantity"), "Expected 'quantity' in $props")
    }

    @Test
    fun testFallbackToPost() {
        val result = resolveForPatch("/users")

        assertNotNull(result)
        assertTrue(result!!.properties.containsKey("email"))
    }

    @Test
    fun testReturnsNullWhenNoSiblings() {
        val result = resolveForPatch("/items/{id}")

        assertNull(result, "Expected null when no sibling operations define a JSON schema")
    }

    @Test
    fun testIgnoresJsonPatchContentTypeInGetResponse() {
        val result = resolveForPatch("/docs/{id}")

        assertNull(result, "Should not use json-patch content type as resource schema")
    }

    @Test
    fun testResolveFromGetResponseViaRef() {
        val result = resolveForPatch("/cats/{id}")

        assertNotNull(result)
        val props = result!!.properties
        assertTrue(props.containsKey("breed"), "Expected 'breed' via \$ref resolution, got $props")
        assertTrue(props.containsKey("indoor"), "Expected 'indoor' via \$ref resolution, got $props")
    }
}
