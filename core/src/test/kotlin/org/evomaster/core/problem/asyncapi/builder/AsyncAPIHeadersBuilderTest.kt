package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AsyncAPI 3.0 lets messages declare a `headers` schema independent of
 * the payload (auth tokens, tenant ids, tracing keys, etc.).  Confirms
 * the parser + builder lift the schema into a separate gene tree the EA
 * can mutate without disturbing the payload.
 */
class AsyncAPIHeadersBuilderTest {

    @Test
    fun headersSchemaIsLiftedIntoSeparateParam() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: 1 }
            channels:
              inbound:
                address: tenant.events
                messages:
                  M:
                    ${'$'}ref: '#/components/messages/M'
            operations:
              publishEvent:
                action: receive
                channel: { ${'$'}ref: '#/channels/inbound' }
                messages:
                  - ${'$'}ref: '#/channels/inbound/messages/M'
            components:
              messages:
                M:
                  headers:
                    type: object
                    required: [authorization]
                    properties:
                      authorization:   { type: string }
                      x-tenant-plan:   { type: string, enum: [free, pro, enterprise] }
                      x-trace-id:      { type: string }
                  payload:
                    type: object
                    properties:
                      command: { type: string }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(asyncapi, SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val publish = built.operations["publishEvent"]!!.first()
        assertEquals(AsyncAPIAction.Kind.PUBLISH, publish.kind)

        val headersGene = publish.headersParam()?.primaryGene()
        assertNotNull(headersGene, "headers param should be wired when message declares a headers schema")
        assertTrue(headersGene is ObjectGene, "headers gene should be an ObjectGene; was ${headersGene!!.javaClass.simpleName}")

        // Required field stays unwrapped; optional fields wrap in OptionalGene.
        val obj = headersGene as ObjectGene
        val authField = obj.fields.firstOrNull { it.name == "authorization" }
        val tenantField = obj.fields.firstOrNull { it.name == "x-tenant-plan" }
        assertNotNull(authField, "required `authorization` should be present")
        assertNotNull(tenantField, "optional `x-tenant-plan` should be present")
        assertTrue(tenantField is OptionalGene, "`x-tenant-plan` is optional → should be OptionalGene")

        // The enum on `x-tenant-plan` reaches the gene tree, so the existing
        // ENUM_VALUE_USED machinery now fires for the headers as well.
        val tenantEnum = tenantField!!.flatView().filterIsInstance<EnumGene<*>>().firstOrNull()
        assertNotNull(tenantEnum, "tenant-plan enum must materialise as EnumGene under the optional wrapper")
        assertTrue(tenantEnum!!.values.toSet().containsAll(listOf("free", "pro", "enterprise")))
    }

    @Test
    fun messagesWithoutHeadersDoNotWireAHeadersParam() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["sendNcsRequest"]!!.first()

        assertNull(publish.headersParam(), "rest-kafka-ncs has no headers schema; param must not be wired")
    }
}
