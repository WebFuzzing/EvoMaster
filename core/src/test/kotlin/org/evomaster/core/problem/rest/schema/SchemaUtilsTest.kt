package org.evomaster.core.problem.rest.schema

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaUtilsTest {

    private fun parse(json: String): RestSchema =
        RestSchema(OpenApiAccess.parseOpenApi(json.trimIndent(), SchemaLocation.MEMORY))

    @Test
    fun testExtractPutRequestSchemaFields_inlineProperties() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "put": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "name": {"type": "string"},
                              "email": {"type": "string"}
                            }
                          }
                        }
                      }
                    },
                    "responses": {"200": {"description": "ok"}}
                  }
                }
              }
            }
        """)
        assertEquals(setOf("name", "email"), SchemaUtils.extractPutRequestSchemaFields(schema, "/x"))
    }

    @Test
    fun testExtractPutRequestSchemaFields_topLevelRef() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "put": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {"${'$'}ref": "#/components/schemas/User"}
                        }
                      }
                    },
                    "responses": {"200": {"description": "ok"}}
                  }
                }
              },
              "components": {
                "schemas": {
                  "User": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string"},
                      "role": {"type": "string"}
                    }
                  }
                }
              }
            }
        """)
        assertEquals(setOf("name", "role"), SchemaUtils.extractPutRequestSchemaFields(schema, "/x"))
    }

    @Test
    fun testExtractPutRequestSchemaFields_allOfComposition() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "put": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "allOf": [
                              {"${'$'}ref": "#/components/schemas/Base"},
                              {
                                "type": "object",
                                "properties": {
                                  "extra": {"type": "string"}
                                }
                              }
                            ]
                          }
                        }
                      }
                    },
                    "responses": {"200": {"description": "ok"}}
                  }
                }
              },
              "components": {
                "schemas": {
                  "Base": {
                    "type": "object",
                    "properties": {
                      "id": {"type": "string"},
                      "name": {"type": "string"}
                    }
                  }
                }
              }
            }
        """)
        assertEquals(setOf("id", "name", "extra"), SchemaUtils.extractPutRequestSchemaFields(schema, "/x"))
    }

    @Test
    fun testExtractPutRequestSchemaFields_nestedAllOf() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "put": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {"${'$'}ref": "#/components/schemas/Outer"}
                        }
                      }
                    },
                    "responses": {"200": {"description": "ok"}}
                  }
                }
              },
              "components": {
                "schemas": {
                  "Inner": {
                    "type": "object",
                    "properties": {"a": {"type": "string"}}
                  },
                  "Outer": {
                    "allOf": [
                      {"${'$'}ref": "#/components/schemas/Inner"},
                      {
                        "type": "object",
                        "properties": {"b": {"type": "string"}}
                      }
                    ]
                  }
                }
              }
            }
        """)
        assertEquals(setOf("a", "b"), SchemaUtils.extractPutRequestSchemaFields(schema, "/x"))
    }

    @Test
    fun testExtractPutRequestSchemaFields_oneOfIgnored() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "put": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "oneOf": [
                              {"type": "object", "properties": {"a": {"type": "string"}}},
                              {"type": "object", "properties": {"b": {"type": "string"}}}
                            ]
                          }
                        }
                      }
                    },
                    "responses": {"200": {"description": "ok"}}
                  }
                }
              }
            }
        """)
        // oneOf/anyOf are intentionally not merged; with no top-level properties result is empty.
        assertTrue(SchemaUtils.extractPutRequestSchemaFields(schema, "/x").isEmpty())
    }

    @Test
    fun testExtractPutRequestSchemaFields_pathMissing() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {}
            }
        """)
        assertTrue(SchemaUtils.extractPutRequestSchemaFields(schema, "/missing").isEmpty())
    }

    @Test
    fun testExtractPutRequestSchemaFields_putOperationMissing() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "get": {"responses": {"200": {"description": "ok"}}}
                }
              }
            }
        """)
        assertTrue(SchemaUtils.extractPutRequestSchemaFields(schema, "/x").isEmpty())
    }

    @Test
    fun testExtractPutRequestSchemaFields_requestBodyMissing() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "put": {"responses": {"200": {"description": "ok"}}}
                }
              }
            }
        """)
        assertTrue(SchemaUtils.extractPutRequestSchemaFields(schema, "/x").isEmpty())
    }

    @Test
    fun testExtractGetResponseSchemaFields_2xxFirstMatch() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "get": {
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "id": {"type": "string"},
                                "name": {"type": "string"}
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """)
        assertEquals(setOf("id", "name"), SchemaUtils.extractGetResponseSchemaFields(schema, "/x"))
    }

    @Test
    fun testExtractGetResponseSchemaFields_defaultFallback() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "get": {
                    "responses": {
                      "default": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {"only": {"type": "string"}}
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """)
        assertEquals(setOf("only"), SchemaUtils.extractGetResponseSchemaFields(schema, "/x"))
    }

    @Test
    fun testExtractGetResponseSchemaFields_responseAllOf() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "get": {
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {
                              "allOf": [
                                {"${'$'}ref": "#/components/schemas/Base"},
                                {"type": "object", "properties": {"createdAt": {"type": "string"}}}
                              ]
                            }
                          }
                        }
                      }
                    }
                  }
                }
              },
              "components": {
                "schemas": {
                  "Base": {
                    "type": "object",
                    "properties": {"id": {"type": "string"}, "name": {"type": "string"}}
                  }
                }
              }
            }
        """)
        assertEquals(
            setOf("id", "name", "createdAt"),
            SchemaUtils.extractGetResponseSchemaFields(schema, "/x")
        )
    }

    @Test
    fun testCollectPropertyNames_cycleSafe() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {},
              "components": {
                "schemas": {
                  "Node": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string"},
                      "child": {"${'$'}ref": "#/components/schemas/Node"}
                    }
                  }
                }
              }
            }
        """)
        val node = schema.main.schemaParsed.components.schemas["Node"]!!
        // self-referencing schema must not blow up; only top-level property names are returned
        assertEquals(setOf("name", "child"), SchemaUtils.collectPropertyNames(schema, node))
    }

    @Test
    fun testCollectPropertyNames_allOfSelfReference() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {},
              "components": {
                "schemas": {
                  "Node": {
                    "allOf": [
                      {"${'$'}ref": "#/components/schemas/Node"},
                      {"type": "object", "properties": {"name": {"type": "string"}}}
                    ]
                  }
                }
              }
            }
        """)
        val node = schema.main.schemaParsed.components.schemas["Node"]!!
        // allOf contains a self-$ref; visitedRefs must break the recursion without blowing up.
        assertEquals(setOf("name"), SchemaUtils.collectPropertyNames(schema, node))
    }
}
