package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.collection.EnumGene
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AsyncAPI 3.0 protocol bindings — confirm the parser + builder honour
 * `messages.<id>.bindings.kafka.key` and lift it into the action's
 * KEY_PARAM gene so the EA can mutate routing keys (and thereby pin
 * messages to specific partitions).
 */
class AsyncAPIKafkaBindingsTest {

    private val asyncapiWithKafkaKey = """
        asyncapi: 3.0.0
        info: { title: t, version: 1 }
        channels:
          inbound:
            address: orders.partitioned
            messages:
              M:
                ${'$'}ref: '#/components/messages/M'
        operations:
          publishOrder:
            action: receive
            channel: { ${'$'}ref: '#/channels/inbound' }
            messages:
              - ${'$'}ref: '#/channels/inbound/messages/M'
        components:
          messages:
            M:
              bindings:
                kafka:
                  key:
                    type: string
                    enum: [tenant-a, tenant-b, tenant-c]
              payload:
                type: object
                properties:
                  id: { type: string }
    """.trimIndent()

    @Test
    fun parserCapturesKafkaKeyBinding() {
        val schema = AsyncAPIAccess.parse(asyncapiWithKafkaKey,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val message = schema.messages["M"]!!
        assertNotNull(message.kafkaKeyInline, "kafka.key binding must be parsed")
    }

    @Test
    fun builderProducesKeyParamWithEnumGene() {
        val schema = AsyncAPIAccess.parse(asyncapiWithKafkaKey,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishOrder"]!!.first()

        val keyParam = publish.keyParam()
        assertNotNull(keyParam, "Kafka key binding must materialise as KEY_PARAM")
        val enumGene = keyParam!!.primaryGene().flatView().filterIsInstance<EnumGene<*>>().firstOrNull()
        assertNotNull(enumGene, "key gene tree should expose an EnumGene for the routing-key enum")
        assertTrue(enumGene!!.values.toSet().containsAll(listOf("tenant-a", "tenant-b", "tenant-c")))
    }

    @Test
    fun messagesWithoutKafkaBindingHaveNoKeyParam() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["sendNcsRequest"]!!.first()
        assertNull(publish.keyParam(), "rest-kafka-ncs has no kafka.key binding")
    }
}
