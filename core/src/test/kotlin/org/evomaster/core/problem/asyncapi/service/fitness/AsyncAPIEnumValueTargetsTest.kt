package org.evomaster.core.problem.asyncapi.service.fitness

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.builder.AsyncAPIActionBuilder
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function tests for [AbstractAsyncAPIFitness.enumValueTargets].
 * Spinning up a broker just to assert target ids would be wasteful — the
 * helper takes a sampled gene and returns target strings, so we drive it
 * directly from a parsed schema fixture.
 */
class AsyncAPIEnumValueTargetsTest {

    @Test
    fun emitsOneTargetPerEnumGeneInThePayload() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["sendNcsRequest"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()

        // Pin every EnumGene to its first allowed value so the test is
        // deterministic regardless of the random initialiser default.
        walk(payload).filterIsInstance<EnumGene<*>>().forEach { it.index = 0 }

        val targets = AbstractAsyncAPIFitness.enumValueTargets(publish, payload)

        // The schema declares exactly one enum (`operation`), so we expect
        // one target with the channel/op/path/value all rendered.
        assertEquals(1, targets.size, "expected one target per EnumGene; got: $targets")
        val t = targets.single()
        assertTrue(t.startsWith("ENUM_VALUE_USED:requests.ncs:sendNcsRequest:operation="), "unexpected id: $t")
    }

    @Test
    fun coversEveryEnumValueAcrossSamples() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["sendNcsRequest"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()

        val operationEnum = walk(payload).filterIsInstance<EnumGene<*>>().first { it.name == "operation" }

        val seen = mutableSetOf<String>()
        for (i in operationEnum.values.indices) {
            operationEnum.index = i
            seen.addAll(AbstractAsyncAPIFitness.enumValueTargets(publish, payload))
        }

        // One target per enum literal — ceiling lifts from 1 (DELIVERY_OK on
        // the operation) to N (one per branch the schema explicitly names).
        assertEquals(operationEnum.values.size, seen.size, "every literal should produce a distinct target; got: $seen")
        assertTrue(seen.all { it.startsWith("ENUM_VALUE_USED:requests.ncs:sendNcsRequest:operation=") })
    }

    @Test
    fun coversBothBooleanStatesAcrossSamples() {
        // Hand-rolled schema with a required boolean.  The helper should
        // emit one target per (gene, sampled-value) pair so the EA has a
        // gradient pulling it through both `true` and `false`.
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: 1 }
            channels:
              inbound:
                address: cmd.topic
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              publishCommand:
                action: send
                channel: { ${'$'}ref: '#/channels/inbound' }
                messages: [ { ${'$'}ref: '#/channels/inbound/messages/M' } ]
            components:
              messages:
                M:
                  payload:
                    type: object
                    required: [urgent]
                    properties:
                      urgent: { type: boolean }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(asyncapi, SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishCommand"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()
        val booleanGene = walk(payload).filterIsInstance<BooleanGene>().single { it.name == "urgent" }

        val seen = mutableSetOf<String>()
        for (v in listOf(true, false)) {
            booleanGene.value = v
            seen.addAll(AbstractAsyncAPIFitness.booleanValueTargets(publish, payload))
        }

        // Two distinct targets — one per sampled boolean state.
        assertEquals(
            setOf(
                "BOOLEAN_VALUE_USED:cmd.topic:publishCommand:urgent=true",
                "BOOLEAN_VALUE_USED:cmd.topic:publishCommand:urgent=false"
            ),
            seen
        )
    }

    @Test
    fun booleanTargetsAreEmptyWhenNoBooleanGenesArePresent() {
        // rest-kafka-ncs's payload has no boolean fields — the helper must
        // not invent any.
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["sendNcsRequest"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()

        assertEquals(emptyList<String>(), AbstractAsyncAPIFitness.booleanValueTargets(publish, payload))
    }

    private fun walk(root: Gene): Sequence<Gene> = root.flatView().asSequence()
}
