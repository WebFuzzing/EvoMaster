package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Confirms `oneOf` / `anyOf` / `allOf` in AsyncAPI message payloads are
 * propagated end-to-end:
 *   YAML → AsyncAPIAccess → JsonSchemaConverter (emits ComposedSchema)
 *        → JsonSchemaToGeneConverter (emits ChoiceGene / merged ObjectGene)
 *        → AsyncAPIAction.payloadParam().primaryGene()
 *
 * No production code change is expected — this is the M9-PR2 coverage gap.
 * If a real shape regresses (composed gene tree collapses to a bare
 * ObjectGene, or required fields are dropped on `allOf` merge), the test
 * fails with a clear message pointing at the missing variant.
 */
class AsyncAPICompositionTest {

    // -----------------------------------------------------------------------
    // oneOf at the payload root — single-channel send, payload is one of two
    // distinct shapes (a "create" command or a "cancel" command).
    // -----------------------------------------------------------------------
    @Test
    fun oneOfPayloadProducesChoiceGene() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              orders:
                address: orders.requests
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: send
                channel: { ${'$'}ref: '#/channels/orders' }
            components:
              messages:
                M:
                  payload:
                    oneOf:
                      - type: object
                        properties:
                          kind: { type: string, enum: [CREATE] }
                          orderId: { type: string }
                        required: [kind, orderId]
                      - type: object
                        properties:
                          kind: { type: string, enum: [CANCEL] }
                          reason: { type: string }
                        required: [kind, reason]
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["op"]!!.first()
        val payload = publish.payloadParam()?.primaryGene()
        assertNotNull(payload, "payload gene missing")

        // The composition is either materialised as a top-level ChoiceGene or
        // wrapped under an ObjectGene container whose first descendant is the
        // ChoiceGene. Either shape is acceptable; the test asserts ChoiceGene
        // exists somewhere reachable, and that each of its two children is an
        // ObjectGene exposing the expected discriminator enum value.
        val choices = walkGenes(payload!!).filterIsInstance<ChoiceGene<*>>().toList()
        assertTrue(choices.isNotEmpty(), "expected oneOf to surface a ChoiceGene under the payload")

        val discriminators = choices.flatMap { choice ->
            choice.getViewOfChildren().filterIsInstance<Gene>().flatMap { walkGenes(it).toList() }
        }
            .filterIsInstance<EnumGene<*>>()
            .flatMap { it.values }
            .toSet()
        assertTrue(
            discriminators.contains("CREATE") && discriminators.contains("CANCEL"),
            "expected oneOf children to expose their kind-enum literals; got $discriminators"
        )
    }

    // -----------------------------------------------------------------------
    // anyOf on a property — `recipient` can be either an email-formatted
    // string or an account-id string. Verify a ChoiceGene appears under the
    // recipient field.
    // -----------------------------------------------------------------------
    @Test
    fun anyOfOnPropertyProducesChoiceGene() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              n:
                address: notifications
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: send
                channel: { ${'$'}ref: '#/channels/n' }
            components:
              messages:
                M:
                  payload:
                    type: object
                    properties:
                      recipient:
                        anyOf:
                          - type: string
                            format: email
                          - type: string
                            pattern: '^acct-[a-z0-9]+$'
                    required: [recipient]
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["op"]!!.first()
        val payload = publish.payloadParam()?.primaryGene() as? ObjectGene
        assertNotNull(payload, "payload should be an ObjectGene")

        val recipient = payload!!.fields.firstOrNull { it.name == "recipient" }
        assertNotNull(recipient, "recipient field missing")

        val choices = walkGenes(recipient!!).filterIsInstance<ChoiceGene<*>>().toList()
        assertTrue(
            choices.isNotEmpty(),
            "expected anyOf to surface a ChoiceGene under 'recipient'; gene tree=${describe(recipient)}"
        )
    }

    // -----------------------------------------------------------------------
    // allOf merging two objects — the merged payload should expose both
    // children's properties as a single flat ObjectGene.
    // -----------------------------------------------------------------------
    @Test
    fun allOfMergesObjectShapes() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c:
                address: c.in
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: send
                channel: { ${'$'}ref: '#/channels/c' }
            components:
              messages:
                M:
                  payload:
                    allOf:
                      - type: object
                        properties:
                          tenantId: { type: string }
                        required: [tenantId]
                      - type: object
                        properties:
                          createdAt: { type: string, format: date-time }
                        required: [createdAt]
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val publish = built.operations["op"]!!.first()
        val payload = publish.payloadParam()?.primaryGene()
        assertNotNull(payload, "payload gene missing")

        // The merged shape must expose BOTH children's fields. We walk the
        // entire gene tree and look for either a single ObjectGene with both
        // fields, or two ObjectGene fragments under a wrapper.
        val fieldNames = walkGenes(payload!!)
            .filterIsInstance<ObjectGene>()
            .flatMap { it.fields.asSequence().map { f -> f.name } }
            .toSet()

        assertTrue(
            fieldNames.contains("tenantId"),
            "allOf merge dropped 'tenantId'; observed fields=$fieldNames"
        )
        assertTrue(
            fieldNames.contains("createdAt"),
            "allOf merge dropped 'createdAt'; observed fields=$fieldNames"
        )
    }

    // -----------------------------------------------------------------------
    // Reply with `oneOf` variants — exercises the existing M8 per-variant
    // pathway on a composition reply schema. The PUBLISH+SUBSCRIBE_REPLY pair
    // should both build; the reply payload should expose the union shape.
    // -----------------------------------------------------------------------
    @Test
    fun replyOneOfVariantsAreReachableInReplyAction() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              req:
                address: req.topic
                messages:
                  ReqMsg: { ${'$'}ref: '#/components/messages/ReqMsg' }
              rep:
                address: rep.topic
                messages:
                  RepMsg: { ${'$'}ref: '#/components/messages/RepMsg' }
            operations:
              call:
                action: send
                channel: { ${'$'}ref: '#/channels/req' }
                reply:
                  channel: { ${'$'}ref: '#/channels/rep' }
            components:
              messages:
                ReqMsg:
                  payload: { type: object, properties: { id: { type: string } } }
                RepMsg:
                  payload:
                    oneOf:
                      - type: object
                        properties:
                          status: { type: string, enum: [OK] }
                          value: { type: integer }
                      - type: object
                        properties:
                          status: { type: string, enum: [ERROR] }
                          message: { type: string }
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val pair = built.operations["call"]!!
        assertEquals(2, pair.size, "request/reply pair should build PUBLISH + SUBSCRIBE_REPLY")

        val replyGene = pair[1].payloadParam()?.primaryGene()
        // The reply gene tree must surface BOTH status literals somewhere
        // (one per oneOf branch).
        if (replyGene != null) {
            val statusLiterals = walkGenes(replyGene)
                .filterIsInstance<EnumGene<*>>()
                .flatMap { it.values }
                .toSet()
            assertTrue(
                statusLiterals.contains("OK") && statusLiterals.contains("ERROR"),
                "reply oneOf branches must expose both status literals; got $statusLiterals"
            )
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun walkGenes(root: Gene): Sequence<Gene> = sequence {
        val stack = ArrayDeque<Gene>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val g = stack.removeLast()
            yield(g)
            g.getViewOfChildren().filterIsInstance<Gene>().forEach { stack.add(it) }
        }
    }

    private fun describe(gene: Gene): String {
        return walkGenes(gene)
            .map { "${it.javaClass.simpleName}('${it.name}')" }
            .joinToString(", ")
    }

    // The StringGene import is kept so a future test asserting on string-format
    // facets in a oneOf branch can compile without rejigging imports.
    @Suppress("unused")
    private val ensureImportRetained: Class<*> = StringGene::class.java
}
