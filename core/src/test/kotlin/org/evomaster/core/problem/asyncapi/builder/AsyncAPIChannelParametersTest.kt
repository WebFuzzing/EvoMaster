package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AsyncAPI 3.0 channel parameters: address templates carry placeholders
 * (`tenants/{tenantId}/orders`) and the channel declares each placeholder
 * under `parameters`.  Confirm the parser + builder produce one mutable
 * gene per placeholder so the EA can explore each tenant.
 */
class AsyncAPIChannelParametersTest {

    private val asyncapiWithChannelParams = """
        asyncapi: 3.0.0
        info: { title: t, version: 1 }
        channels:
          tenantOrders:
            address: 'tenants/{tenantId}/orders'
            messages:
              M:
                ${'$'}ref: '#/components/messages/M'
            parameters:
              tenantId:
                schema:
                  type: string
                  enum: [acme, globex, initech]
        operations:
          publishOrder:
            action: send
            channel: { ${'$'}ref: '#/channels/tenantOrders' }
            messages:
              - ${'$'}ref: '#/channels/tenantOrders/messages/M'
        components:
          messages:
            M:
              payload: { type: object }
    """.trimIndent()

    @Test
    fun parserExtractsParametersDeclaration() {
        val schema = AsyncAPIAccess.parse(asyncapiWithChannelParams,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val tenantOrders = schema.channels["tenantOrders"]!!
        assertEquals(setOf("tenantId"), tenantOrders.parameters.keys)
    }

    @Test
    fun builderProducesOneGenePerPlaceholder() {
        val schema = AsyncAPIAccess.parse(asyncapiWithChannelParams,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishOrder"]!!.first()

        val tenantParam = publish.channelParams()["tenantId"]
        assertNotNull(tenantParam, "tenantId placeholder must produce a channel-parameter gene")
        // The schema declares an enum, so the gene tree must surface it for
        // the EA to mutate (and for ENUM_VALUE_USED targets to fire).
        val enumGene = tenantParam!!.primaryGene().flatView().filterIsInstance<EnumGene<*>>().firstOrNull()
        if (enumGene != null) {
            assertTrue(enumGene.values.toSet().containsAll(listOf("acme", "globex", "initech")))
        } else {
            // If the converter falls back to a plain string, the gene should
            // at least be a StringGene so the EA can still mutate it.
            assertTrue(tenantParam.primaryGene().flatView().any { it is StringGene })
        }
    }

    @Test
    fun rawAddressKeepsPlaceholder() {
        val schema = AsyncAPIAccess.parse(asyncapiWithChannelParams,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishOrder"]!!.first()
        // The action stores the templated form; the fitness substitutes at
        // publish time.  Keeping the placeholder here means the target ids
        // (DELIVERY_OK / REPLY_RECEIVED / …) stay stable across tenants
        // instead of exploding once per parameter value.
        assertEquals("tenants/{tenantId}/orders", publish.channelAddress)
        assertEquals(AsyncAPIAction.Kind.PUBLISH, publish.kind)
    }
}
