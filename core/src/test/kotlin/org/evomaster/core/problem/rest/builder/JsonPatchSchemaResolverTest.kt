package org.evomaster.core.problem.rest.builder

import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchSchemaResolverTest {

    private fun parse(json: String): RestSchema =
        RestSchema(OpenApiAccess.parseOpenApi(json.trimIndent(), SchemaLocation.MEMORY))

    private fun minimalSpec(pathsBlock: String) = """
        {
          "openapi": "3.0.0",
          "info": {"title": "t", "version": "1"},
          "paths": { $pathsBlock }
        }
    """.trimIndent()

    private fun resolveForPatch(
        schema: RestSchema,
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
        val schema = parse(minimalSpec("""
            "/pets/{id}": {
              "get": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "responses": {
                  "200": {
                    "description": "ok",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "name": {"type": "string"},
                            "age":  {"type": "integer"}
                          }
                        }
                      }
                    }
                  }
                }
              },
              "patch": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "required": true,
                  "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                },
                "responses": {"200": {"description": "ok"}}
              }
            }
        """))

        val messages = mutableListOf<String>()

        val result = resolveForPatch(schema, "/pets/{id}", messages)

        assertNotNull(result)
        assertTrue(messages.isEmpty(), "Unexpected messages: $messages")
        val props = result!!.properties
        assertNotNull(props)
        assertTrue(props.containsKey("name"), "Expected property 'name'")
        assertTrue(props.containsKey("age"), "Expected property 'age'")
    }

    @Test
    fun testPreferGetOverPut() {
        val schema = parse(minimalSpec("""
            "/x/{id}": {
              "get": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "responses": {
                  "200": {
                    "content": {"application/json": {"schema": {"type": "object", "properties": {"fromGet": {"type": "string"}}}}}
                  }
                }
              },
              "put": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "content": {"application/json": {"schema": {"type": "object", "properties": {"fromPut": {"type": "string"}}}}}
                },
                "responses": {"200": {"description": "ok"}}
              },
              "patch": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                },
                "responses": {"200": {"description": "ok"}}
              }
            }
        """))

        val result = resolveForPatch(schema, "/x/{id}")

        assertNotNull(result)
        val props = result!!.properties
        assertTrue(props.containsKey("fromGet"), "Should prefer GET schema, got $props")
        assertFalse(props.containsKey("fromPut"))
    }

    @Test
    fun testFallbackToPut() {
        val schema = parse(minimalSpec("""
            "/orders/{id}": {
              "put": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "content": {"application/json": {"schema": {"type": "object", "properties": {"product": {"type": "string"}, "quantity": {"type": "integer"}}}}}
                },
                "responses": {"200": {"description": "ok"}}
              },
              "patch": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                },
                "responses": {"200": {"description": "ok"}}
              }
            }
        """))

        val result = resolveForPatch(schema, "/orders/{id}")

        assertNotNull(result)
        val props = result!!.properties
        assertTrue(props.containsKey("product"), "Expected 'product' in $props")
        assertTrue(props.containsKey("quantity"), "Expected 'quantity' in $props")
    }

    @Test
    fun testFallbackToPost() {
        val schema = parse(minimalSpec("""
            "/users": {
              "post": {
                "requestBody": {
                  "content": {"application/json": {"schema": {"type": "object", "properties": {"email": {"type": "string"}}}}}
                },
                "responses": {"200": {"description": "ok"}}
              },
              "patch": {
                "requestBody": {
                  "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                },
                "responses": {"200": {"description": "ok"}}
              }
            }
        """))

        val result = resolveForPatch(schema, "/users")

        assertNotNull(result)
        assertTrue(result!!.properties.containsKey("email"))
    }

    @Test
    fun testReturnsNullWhenNoSiblings() {
        val schema = parse(minimalSpec("""
            "/items/{id}": {
              "patch": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                },
                "responses": {"200": {"description": "ok"}}
              }
            }
        """))

        val result = resolveForPatch(schema, "/items/{id}")

        assertNull(result, "Expected null when no sibling operations define a JSON schema")
    }

    @Test
    fun testIgnoresJsonPatchContentTypeInGetResponse() {
        val schema = parse(minimalSpec("""
            "/docs/{id}": {
              "get": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "responses": {
                  "200": {
                    "content": {
                      "application/json-patch+json": {
                        "schema": {"type": "array", "items": {"type": "object"}}
                      }
                    }
                  }
                }
              },
              "patch": {
                "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                "requestBody": {
                  "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                },
                "responses": {"200": {"description": "ok"}}
              }
            }
        """))

        val result = resolveForPatch(schema, "/docs/{id}")

        assertNull(result, "Should not use json-patch content type as resource schema")
    }

    @Test
    fun testResolveFromGetResponseViaRef() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/cats/{id}": {
                  "get": {
                    "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": {"${'$'}ref": "#/components/schemas/Cat"}
                          }
                        }
                      }
                    }
                  },
                  "patch": {
                    "parameters": [{"name":"id","in":"path","required":true,"schema":{"type":"integer"}}],
                    "requestBody": {
                      "content": {"application/json-patch+json": {"schema": {"type": "array", "items": {"type": "object"}}}}
                    },
                    "responses": {"200": {"description": "ok"}}
                  }
                }
              },
              "components": {
                "schemas": {
                  "Cat": {
                    "type": "object",
                    "properties": {
                      "breed": {"type": "string"},
                      "indoor": {"type": "boolean"}
                    }
                  }
                }
              }
            }
        """.trimIndent())

        val result = resolveForPatch(schema, "/cats/{id}")

        assertNotNull(result)
        val props = result!!.properties
        assertTrue(props.containsKey("breed"), "Expected 'breed' via \$ref resolution, got $props")
        assertTrue(props.containsKey("indoor"), "Expected 'indoor' via \$ref resolution, got $props")
    }
}
