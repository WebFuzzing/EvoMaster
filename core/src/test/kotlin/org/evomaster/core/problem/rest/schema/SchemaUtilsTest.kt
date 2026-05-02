package org.evomaster.core.problem.rest.schema

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.HttpVerb
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
        assertEquals(setOf("name", "email"), SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT))
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
        assertEquals(setOf("name", "role"), SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT))
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
        assertEquals(setOf("id", "name", "extra"), SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT))
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
        assertEquals(setOf("a", "b"), SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT))
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
        assertTrue(SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT).isEmpty())
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
        assertTrue(SchemaUtils.extractRequestBodySchemaFields(schema, "/missing", HttpVerb.PUT).isEmpty())
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
        assertTrue(SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT).isEmpty())
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
        assertEquals(setOf("id", "name"), SchemaUtils.extractResponseSchemaFields(schema, "/x", HttpVerb.GET, statusMatcher = SchemaUtils.statusGroupMatcher(StatusGroup.G_2xx)))
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
        assertEquals(setOf("only"), SchemaUtils.extractResponseSchemaFields(schema, "/x", HttpVerb.GET, statusMatcher = SchemaUtils.statusGroupMatcher(StatusGroup.G_2xx)))
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

    // -------------------------------------------------------------------------
    // Generic extractRequestBodySchemaFields / extractResponseSchemaFields
    // -------------------------------------------------------------------------

    @Test
    fun testExtractRequestBodySchemaFields_postVerb() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "post": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {"a": {"type": "string"}, "b": {"type": "string"}}
                          }
                        }
                      }
                    },
                    "responses": {"201": {"description": "created"}}
                  }
                }
              }
            }
        """)
        assertEquals(
            setOf("a", "b"),
            SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.POST)
        )
    }

    @Test
    fun testExtractRequestBodySchemaFields_verbAbsent() {
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
        assertTrue(
            SchemaUtils.extractRequestBodySchemaFields(schema, "/x", HttpVerb.PUT).isEmpty()
        )
    }

    @Test
    fun testExtractResponseSchemaFields_4xxGroup() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "post": {
                    "responses": {
                      "400": {
                        "description": "bad",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {"errorCode": {"type": "string"}}
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
        assertEquals(
            setOf("errorCode"),
            SchemaUtils.extractResponseSchemaFields(
                schema, "/x", HttpVerb.POST,
                statusMatcher = SchemaUtils.statusGroupMatcher(StatusGroup.G_4xx)
            )
        )
    }

    @Test
    fun testExtractResponseSchemaFields_exactStatus201() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "post": {
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {"type": "object", "properties": {"ok": {"type": "string"}}}
                          }
                        }
                      },
                      "201": {
                        "description": "created",
                        "content": {
                          "application/json": {
                            "schema": {"type": "object", "properties": {"id": {"type": "string"}}}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """)
        // 2xx group: would pick the FIRST 2xx (likely "200"); exact 201 forces the 201 schema.
        assertEquals(
            setOf("id"),
            SchemaUtils.extractResponseSchemaFields(
                schema, "/x", HttpVerb.POST,
                statusMatcher = SchemaUtils.statusCodeMatcher(201)
            )
        )
    }

    @Test
    fun testExtractResponseSchemaFields_anyOf200or201() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "post": {
                    "responses": {
                      "201": {
                        "description": "created",
                        "content": {
                          "application/json": {
                            "schema": {"type": "object", "properties": {"id": {"type": "string"}}}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """)
        // 200 not present, 201 is — matcher must pick 201
        assertEquals(
            setOf("id"),
            SchemaUtils.extractResponseSchemaFields(
                schema, "/x", HttpVerb.POST,
                statusMatcher = SchemaUtils.statusCodesMatcher(200, 201)
            )
        )
    }

    @Test
    fun testExtractResponseSchemaFields_noMatch_noFallback() {
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
                            "schema": {"type": "object", "properties": {"only": {"type": "string"}}}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """)
        // No 2xx and fallbackToDefault=false → empty
        assertTrue(
            SchemaUtils.extractResponseSchemaFields(
                schema, "/x", HttpVerb.GET,
                statusMatcher = SchemaUtils.statusGroupMatcher(StatusGroup.G_2xx),
                fallbackToDefault = false
            ).isEmpty()
        )
    }

    @Test
    fun testExtractResponseSchemaFields_3xxGroup() {
        val schema = parse("""
            {
              "openapi": "3.0.0",
              "info": {"title": "t", "version": "1"},
              "paths": {
                "/x": {
                  "get": {
                    "responses": {
                      "302": {
                        "description": "redirect",
                        "content": {
                          "application/json": {
                            "schema": {"type": "object", "properties": {"location": {"type": "string"}}}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """)
        assertEquals(
            setOf("location"),
            SchemaUtils.extractResponseSchemaFields(
                schema, "/x", HttpVerb.GET,
                statusMatcher = SchemaUtils.statusGroupMatcher(StatusGroup.G_3xx)
            )
        )
    }
}
