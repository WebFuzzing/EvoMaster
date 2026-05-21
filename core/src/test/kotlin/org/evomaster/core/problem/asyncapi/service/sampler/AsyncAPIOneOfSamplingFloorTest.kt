package org.evomaster.core.problem.asyncapi.service.sampler

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.builder.AsyncAPIActionBuilder
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M11-PR5: the `oneOf`/`anyOf` sampling floor must visit every variant of a
 * declared composition at least once within the first K samples of a
 * template (K = variant count). After the floor is satisfied the cursor
 * keeps incrementing but no longer overrides the random pick.
 *
 * The test wires up an AsyncAPI schema with a 3-variant `oneOf` payload,
 * samples the action repeatedly through the same template counter, and
 * asserts each variant index gets visited.
 */
class AsyncAPIOneOfSamplingFloorTest {

    @Test
    fun floorVisitsEveryVariantWithinKSamples() {
        val template = buildOneOfPublishAction(variantCount = 3)
        val counters = HashMap<String, Int>()
        val visited = mutableSetOf<Int>()

        // Sample the template 3 times (K = 3 variants). Each sample copies
        // the action and randomises its genes; the floor then overrides the
        // ChoiceGene's index to `cursor % size`. After three rounds every
        // index must have been visited at least once.
        repeat(3) {
            val sample = template.copy() as AsyncAPIAction
            sample.doInitialize(Randomness().apply { updateSeed(42L + it) })
            val cursor = AsyncAPISampler.applyOneOfFloorForAction(
                sample, counters, "orderTemplate"
            )
            assertTrue(cursor in 0..2, "cursor should land in [0,2]; was $cursor")
            visited.add(activeChoiceIndex(sample))
        }

        assertEquals(setOf(0, 1, 2), visited,
            "after K=3 samples the floor must visit every oneOf variant; visited=$visited"
        )
    }

    @Test
    fun floorDoesNotOverrideAfterTheWindow() {
        val template = buildOneOfPublishAction(variantCount = 2)
        val counters = HashMap<String, Int>()

        // First 2 samples → cursors 0, 1 (override applied).
        val cursors = (0 until 4).map {
            val sample = template.copy() as AsyncAPIAction
            sample.doInitialize(Randomness().apply { updateSeed(100L + it) })
            AsyncAPISampler.applyOneOfFloorForAction(
                sample, counters, "binaryTemplate"
            )
        }
        assertEquals(listOf(0, 1, 2, 3), cursors,
            "cursor must increment monotonically across calls; got $cursors"
        )
        // Cursors >= size do not override the active gene — the floor is
        // satisfied. The actual index after that is whatever `randomize`
        // (or a subsequent EA mutation) picked; we don't assert on it.
    }

    @Test
    fun actionWithoutOneOfReturnsSentinel() {
        val template = buildPlainPublishAction()
        val cursor = AsyncAPISampler.applyOneOfFloorForAction(
            template, HashMap(), "plainTemplate"
        )
        assertEquals(-1, cursor,
            "actions without ChoiceGenes should signal -1 to skip counter bookkeeping"
        )
    }

    private fun buildOneOfPublishAction(variantCount: Int): AsyncAPIAction {
        val indent = "                      "
        val variantBlock = (1..variantCount).joinToString("\n") { idx ->
            buildString {
                append(indent).append("- type: object\n")
                append(indent).append("  required: [kind]\n")
                append(indent).append("  properties:\n")
                append(indent).append("    kind: { type: string, enum: [V").append(idx).append("] }\n")
                append(indent).append("    payload").append(idx).append(": { type: string }")
            }
        }
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c:
                address: orders.in
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: receive
                channel: { ${'$'}ref: '#/channels/c' }
            components:
              messages:
                M:
                  payload:
                    oneOf:
$variantBlock
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi, SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        return built.operations["op"]!!.first { it.kind == AsyncAPIAction.Kind.PUBLISH }
    }

    private fun buildPlainPublishAction(): AsyncAPIAction {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              c:
                address: orders.in
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              op:
                action: receive
                channel: { ${'$'}ref: '#/channels/c' }
            components:
              messages:
                M:
                  payload:
                    type: object
                    properties:
                      orderId: { type: string }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi, SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        return built.operations["op"]!!.first { it.kind == AsyncAPIAction.Kind.PUBLISH }
    }

    private fun activeChoiceIndex(action: AsyncAPIAction): Int {
        val choices: List<ChoiceGene<*>> = action.seeTopGenes()
            .flatMap { it.flatView() }
            .filterIsInstance<ChoiceGene<*>>()
        check(choices.isNotEmpty()) { "expected a ChoiceGene in the action's gene tree" }
        return choices.first().activeGeneIndex
    }

    // Kotlin compiler warns about an unused import otherwise; this no-op
    // reference keeps Gene in scope for tests that grow to assert gene
    // shapes inside the variant branches.
    @Suppress("unused")
    private val ensureImport: Class<*> = Gene::class.java
}
