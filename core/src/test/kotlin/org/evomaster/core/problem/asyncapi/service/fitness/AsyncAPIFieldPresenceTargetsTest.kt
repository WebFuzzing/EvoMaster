package org.evomaster.core.problem.asyncapi.service.fitness

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.builder.AsyncAPIActionBuilder
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function tests for [AbstractAsyncAPIFitness.fieldPresenceTargets].
 * Exercises a hand-rolled fixture where the request payload has both a
 * required field and an optional one, so we can verify only optional
 * fields produce presence targets and that flipping isActive flips the
 * recorded `=present` / `=absent` half.
 */
class AsyncAPIFieldPresenceTargetsTest {

    private val asyncapiWithOptionalField = """
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
            action: send
            channel: { ${'$'}ref: '#/channels/inbound' }
            messages:
              - ${'$'}ref: '#/channels/inbound/messages/M'
        components:
          messages:
            M:
              payload:
                type: object
                required: [command]
                properties:
                  command: { type: string, enum: [start, stop] }
                  note:    { type: string }
    """.trimIndent()

    @Test
    fun emitsTwoTargetsPerOptionalFieldAcrossPresenceStates() {
        val schema = AsyncAPIAccess.parse(
            asyncapiWithOptionalField,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishCommand"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()

        val noteWrapper = payload.flatView().filterIsInstance<OptionalGene>().single { it.name == "note" }

        noteWrapper.isActive = true
        val active = AbstractAsyncAPIFitness.fieldPresenceTargets(publish, payload).single()
        assertEquals("FIELD_PRESENCE:in.topic:publishCommand:note=present", active)

        noteWrapper.isActive = false
        val inactive = AbstractAsyncAPIFitness.fieldPresenceTargets(publish, payload).single()
        assertEquals("FIELD_PRESENCE:in.topic:publishCommand:note=absent", inactive)
    }

    @Test
    fun requiredFieldsProduceNoPresenceTargets() {
        // The schema declares `command` as required, so RestActionBuilderV3
        // does not wrap it in an OptionalGene.  We must not pretend we have a
        // presence signal for it.
        val schema = AsyncAPIAccess.parse(
            asyncapiWithOptionalField,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["publishCommand"]!!.first()
        val payload = publish.payloadParam()!!.primaryGene()

        val targets = AbstractAsyncAPIFitness.fieldPresenceTargets(publish, payload)

        assertTrue(targets.none { it.contains(":command=") }, "required `command` field should not appear: $targets")
    }
}
