package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AsyncAPIActionBuilderTest {

    /**
     * Round-trip the rest-kafka-ncs fixture through the full pipeline:
     * parse → build actions → inspect the resulting gene tree.  This is the
     * end-to-end check that the JsonSchemaConverter + JsonSchemaToGeneConverter
     * + AsyncAPISchemaRefResolver chain produces something the EA can mutate.
     */
    @Test
    fun buildsPublishAndSubscribeReplyPairForRequestReplyOperation() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val sendOp = built.operations["sendNcsRequest"]
        assertNotNull(sendOp)
        assertEquals(2, sendOp!!.size, "request/reply operation should produce a PUBLISH + SUBSCRIBE_REPLY pair")

        val publish = sendOp[0]
        val subscribe = sendOp[1]
        assertEquals(AsyncAPIAction.Kind.PUBLISH, publish.kind)
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_REPLY, subscribe.kind)
        assertEquals("requests.ncs", publish.channelAddress)
        assertEquals("responses.ncs", subscribe.channelAddress)

        // Both halves of the pair share an evolution-stable pairId.
        assertEquals(publish.pairId, subscribe.pairId)

        // Correlation header was lifted from the AsyncAPI schema declaration
        // `correlationId.location: '$message.header#/evm-correlation-id'`.
        assertEquals("evm-correlation-id", publish.correlationHeaderName)
        assertEquals("evm-correlation-id", subscribe.correlationHeaderName)
    }

    @Test
    fun publishPayloadCarriesAnObjectGeneWithOperationEnumAndArgsBag() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val publish = built.operations["sendNcsRequest"]!!.first()
        val payload = publish.payloadParam()?.primaryGene()
        assertNotNull(payload)
        assertTrue(payload is ObjectGene, "payload should be an ObjectGene; was ${payload!!.javaClass.simpleName}")

        val obj = payload as ObjectGene
        val operationField = obj.fields.firstOrNull { it.name == "operation" }
        assertNotNull(operationField, "RequestPayload.operation must be present")

        // The schema declares an enum on `operation`, so the gene tree should
        // contain an EnumGene<String> somewhere under it.  EnumGene may sit
        // inside an OptionalGene/ChoiceGene wrapper depending on schema
        // facets, so we walk the tree.
        val hasOperationEnum = walkGenes(operationField!!).any {
            (it is EnumGene<*>) && it.values.any { v -> v == "triangle" || v == "bessj" }
        }
        assertTrue(hasOperationEnum, "operation field should carry an EnumGene with the schema's literal values")
    }

    @Test
    fun sendOnlyOperationsProduceSubscribeOutput() {
        // Hand-roll a schema with a single `send` operation and confirm the
        // builder emits one SUBSCRIBE_OUTPUT action. Per AsyncAPI 3.0
        // §4.5, `action: send` means the application (= SUT) sends — i.e.
        // the channel is SUT-produced and the testing tool can only
        // observe it. The M9-PR4 output-observation oracle picks these
        // up; previously they were silently dropped.
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: 1 }
            channels:
              outbound:
                address: out.topic
                messages:
                  M:
                    ${'$'}ref: '#/components/messages/M'
            operations:
              recvOnly:
                action: send
                channel: { ${'$'}ref: '#/channels/outbound' }
            components:
              messages:
                M:
                  payload: { type: object }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val actions = built.operations["recvOnly"]
        assertNotNull(actions)
        assertEquals(1, actions!!.size, "SEND op should produce one SUBSCRIBE_OUTPUT action")
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT, actions[0].kind)
        assertEquals("out.topic", actions[0].channelAddress)
    }

    @Test
    fun simpleSendOperationProducesOnePublishWithoutSubscribeReply() {
        // SEND with no `reply` clause: just one PUBLISH action and no twin.
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: 1 }
            channels:
              inbound:
                address: in.topic
                messages:
                  M:
                    ${'$'}ref: '#/components/messages/M'
            operations:
              fireAndForget:
                action: receive
                channel: { ${'$'}ref: '#/channels/inbound' }
            components:
              messages:
                M:
                  payload: { type: object, properties: { v: { type: string } } }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val actions = built.operations["fireAndForget"]
        assertNotNull(actions)
        assertEquals(1, actions!!.size)
        assertEquals(AsyncAPIAction.Kind.PUBLISH, actions[0].kind)
        assertNull(actions[0].replyBinding)
    }

    private fun walkGenes(root: org.evomaster.core.search.gene.Gene): Sequence<org.evomaster.core.search.gene.Gene> = sequence {
        val stack = ArrayDeque<org.evomaster.core.search.gene.Gene>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val g = stack.removeLast()
            yield(g)
            // The framework's child list view is enough for our purposes; the
            // gene-tree walk just needs to surface every Gene in the subtree.
            g.getViewOfChildren().filterIsInstance<org.evomaster.core.search.gene.Gene>()
                .forEach { stack.add(it) }
        }
    }
}
