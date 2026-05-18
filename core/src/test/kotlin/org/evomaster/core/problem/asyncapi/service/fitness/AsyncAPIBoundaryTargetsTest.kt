package org.evomaster.core.problem.asyncapi.service.fitness

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.builder.AsyncAPIActionBuilder
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function tests for [AbstractAsyncAPIFitness.boundaryTargets].
 * Walks a hand-rolled schema with numeric and string constraints and
 * confirms that targets fire only when the sampled value sits exactly on
 * a schema-declared bound — that's the signal MIO needs to explore the
 * boundary edge.
 */
class AsyncAPIBoundaryTargetsTest {

    private val asyncapiWithBounds = """
        asyncapi: 3.0.0
        info: { title: t, version: 1 }
        channels:
          inbound:
            address: in.topic
            messages:
              M:
                ${'$'}ref: '#/components/messages/M'
        operations:
          publishCommand:
            action: receive
            channel: { ${'$'}ref: '#/channels/inbound' }
            messages:
              - ${'$'}ref: '#/channels/inbound/messages/M'
        components:
          messages:
            M:
              payload:
                type: object
                required: [age, code]
                properties:
                  age:  { type: integer, minimum: 18, maximum: 99 }
                  code: { type: string, minLength: 3, maxLength: 8 }
    """.trimIndent()

    @Test
    fun emitsAtMinAndAtMaxWhenNumericValueLandsOnBound() {
        val (publish, payload) = build()
        val ageGene = payload.flatView().filterIsInstance<IntegerGene>().single { it.name == "age" }

        ageGene.value = 18
        assertTrue(AbstractAsyncAPIFitness.boundaryTargets(publish, payload)
            .contains("BOUNDARY_HIT:in.topic:publishCommand:age=at-min"))

        ageGene.value = 99
        assertTrue(AbstractAsyncAPIFitness.boundaryTargets(publish, payload)
            .contains("BOUNDARY_HIT:in.topic:publishCommand:age=at-max"))

        ageGene.value = 50
        assertEquals(emptyList<String>(),
            AbstractAsyncAPIFitness.boundaryTargets(publish, payload).filter { it.contains(":age=") })
    }

    @Test
    fun emitsAtMinLengthAndAtMaxLengthWhenStringHitsBoundary() {
        val (publish, payload) = build()
        val codeGene = payload.flatView().filterIsInstance<StringGene>().single { it.name == "code" }

        codeGene.value = "abc" // length 3 == minLength
        assertTrue(AbstractAsyncAPIFitness.boundaryTargets(publish, payload)
            .contains("BOUNDARY_HIT:in.topic:publishCommand:code=at-min-length"))

        codeGene.value = "abcdefgh" // length 8 == maxLength
        assertTrue(AbstractAsyncAPIFitness.boundaryTargets(publish, payload)
            .contains("BOUNDARY_HIT:in.topic:publishCommand:code=at-max-length"))

        codeGene.value = "abcde"
        assertEquals(emptyList<String>(),
            AbstractAsyncAPIFitness.boundaryTargets(publish, payload).filter { it.contains(":code=") })
    }

    @Test
    fun unboundedGenesEmitNothing() {
        val schema = AsyncAPIAccess.parse("""
            asyncapi: 3.0.0
            info: { title: t, version: 1 }
            channels:
              c:
                address: t
                messages: { M: { ${'$'}ref: '#/components/messages/M' } }
            operations:
              op:
                action: receive
                channel: { ${'$'}ref: '#/channels/c' }
                messages: [ { ${'$'}ref: '#/channels/c/messages/M' } ]
            components:
              messages:
                M:
                  payload:
                    type: object
                    required: [n, s]
                    properties:
                      n: { type: integer }
                      s: { type: string }
        """.trimIndent(), SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["op"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()

        assertEquals(emptyList<String>(), AbstractAsyncAPIFitness.boundaryTargets(publish, payload))
    }

    private fun build(): Pair<org.evomaster.core.problem.asyncapi.data.AsyncAPIAction,
            org.evomaster.core.search.gene.Gene> {
        val schema = AsyncAPIAccess.parse(asyncapiWithBounds, SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishCommand"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()
        return publish to payload
    }
}
