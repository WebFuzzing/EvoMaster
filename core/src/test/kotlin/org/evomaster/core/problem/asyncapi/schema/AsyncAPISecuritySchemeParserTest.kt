package org.evomaster.core.problem.asyncapi.schema

import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Confirms M9-PR3 parser additions surface `components.securitySchemes` and
 * per-operation `security:` arrays into the [AsyncAPISchema] / [AsyncAPIOperation]
 * model. The broker auth wiring downstream consumes these via
 * `AsyncAPIBrokerAuthInfo`, but the parser layer is the load-bearing piece —
 * if a real-world schema's auth scheme silently disappears here, the engine
 * cannot dispatch it.
 */
class AsyncAPISecuritySchemeParserTest {

    @Test
    fun parsesSaslScramAndX509SchemesAndPerOperationReferences() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              orders:
                address: orders.requests
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              postOrder:
                action: receive
                channel: { ${'$'}ref: '#/channels/orders' }
                security:
                  - { ${'$'}ref: '#/components/securitySchemes/kafkaScram' }
              cancelOrder:
                action: receive
                channel: { ${'$'}ref: '#/channels/orders' }
                security:
                  - { ${'$'}ref: '#/components/securitySchemes/kafkaScram' }
                  - { ${'$'}ref: '#/components/securitySchemes/clientCert' }
            components:
              messages:
                M:
                  name: M
                  payload: { type: object }
              securitySchemes:
                kafkaScram:
                  type: scramSha256
                  description: "Production Kafka cluster (SCRAM-SHA-256)"
                clientCert:
                  type: X509
                  description: "mTLS for the integration cluster"
                tokenBearer:
                  type: http
                  scheme: bearer
                  bearerFormat: JWT
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )

        // Three schemes captured under the model, types lowercased.
        assertEquals(setOf("kafkaScram", "clientCert", "tokenBearer"), schema.securitySchemes.keys)
        assertEquals("scramsha256", schema.securitySchemes["kafkaScram"]!!.type)
        assertEquals("x509", schema.securitySchemes["clientCert"]!!.type)
        assertEquals("http", schema.securitySchemes["tokenBearer"]!!.type)
        assertEquals("bearer", schema.securitySchemes["tokenBearer"]!!.scheme)
        assertEquals("JWT", schema.securitySchemes["tokenBearer"]!!.bearerFormat)
        assertEquals(
            "Production Kafka cluster (SCRAM-SHA-256)",
            schema.securitySchemes["kafkaScram"]!!.description
        )

        // Per-operation security: postOrder references one scheme, cancelOrder two.
        val postOrder = schema.operations["postOrder"]!!
        val cancelOrder = schema.operations["cancelOrder"]!!
        assertEquals(listOf("kafkaScram"), postOrder.security)
        assertEquals(listOf("kafkaScram", "clientCert"), cancelOrder.security)
    }

    @Test
    fun emptySchemaSurfacesEmptyMapsNotNull() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c:
                address: t
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: receive
                channel: { ${'$'}ref: '#/channels/c' }
            components:
              messages:
                M: { payload: { type: object } }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        assertTrue(schema.securitySchemes.isEmpty(), "no securitySchemes -> empty map")
        assertTrue(schema.operations["op"]!!.security.isEmpty(), "no security on op -> empty list")
    }

    @Test
    fun malformedSchemeWithoutTypeIsSkippedNotFatal() {
        // Defensive: a half-written security scheme without a `type` field
        // shouldn't kill the entire parse.
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c:
                address: t
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: receive
                channel: { ${'$'}ref: '#/channels/c' }
            components:
              messages:
                M: { payload: { type: object } }
              securitySchemes:
                halfBaked:
                  description: "type field missing"
                ok:
                  type: plain
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        // halfBaked filtered out, ok kept.
        assertNotNull(schema.securitySchemes["ok"])
        assertNull(schema.securitySchemes["halfBaked"])
    }
}
