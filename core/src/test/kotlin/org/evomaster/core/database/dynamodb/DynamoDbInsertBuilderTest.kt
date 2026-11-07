package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests inference of DynamoDB initialization actions from failed reads. */
class DynamoDbInsertBuilderTest {

    @Test
    fun buildsTypedActionFromSupportedFailedQuery() {
        val actions = DynamoDbInsertBuilder.buildInsertActions(listOf(validQuery()), Randomness())

        assertEquals(1, actions.size)
        val attributes = actions.single().attributes
        assertEquals("WorldCupPlayers", actions.single().tableName)
        assertEquals("Argentina", (attributes[0].gene as StringGene).value)
        assertEquals("10.50", (attributes[1].gene as BigDecimalGene).value.toPlainString())
        assertTrue((attributes[2].gene as BooleanGene).value)
    }

    @Test
    fun skipsInvalidQueriesAndRemovesDuplicateActions() {
        val valid = validQuery()
        val invalidNumber = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "ten"))
        )
        val blankTable = DynamoDbFailedQuery(
            "",
            listOf(DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"))
        )
        val missingType = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto().apply {
                attributeName = "country"
                value = "Argentina"
            })
        )

        val actions = DynamoDbInsertBuilder.buildInsertActions(
            listOf(valid, valid, invalidNumber, blankTable, missingType),
            Randomness()
        )

        assertEquals(1, actions.size)
        assertTrue(
            DynamoDbInsertBuilder.buildInsertActions(
                listOf(valid),
                setOf(actions.single().insertionKey()),
                Randomness()
            ).isEmpty()
        )
    }

    @Test
    fun generatesReproducibleWorldCupKeysWithoutRandomizingInferredValues() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf(
                "recordKey" to DynamoDbKeyScalarTypeDto.STRING,
                "matchNumber" to DynamoDbKeyScalarTypeDto.NUMBER
            )
        }
        val first = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()
        val replay = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()

        assertEquals(first.attributes.map { it.gene.getValueAsRawString() },
            replay.attributes.map { it.gene.getValueAsRawString() })
        assertEquals(listOf("Argentina", "10.50", "true"),
            first.attributes.take(3).map { it.gene.getValueAsRawString() })
        val recordKey = first.attributes[3].gene as StringGene
        assertFalse(recordKey.value.isEmpty())
        assertEquals(1, recordKey.minLength)
        val matchNumber = first.attributes[4].gene as BigDecimalGene
        assertTrue(matchNumber.isLocallyValid())
        assertTrue(matchNumber.value.precision() <= 38)
    }

    @Test
    fun repeatedFailedReadsCanGenerateFreshKeys() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf("recordKey" to DynamoDbKeyScalarTypeDto.STRING)
        }
        val randomness = Randomness()
        val first = DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).single()
        val second = DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).single()

        assertNotEquals(first.attributes.last().gene.getValueAsRawString(),
            second.attributes.last().gene.getValueAsRawString())
    }

    @Test
    fun suppliedKeysDoNotConsumeRandomness() {
        val randomness = Randomness()
        DynamoDbInsertBuilder.buildInsertActions(listOf(validQuery()), randomness)
        assertEquals(Randomness().nextInt(), randomness.nextInt())
    }

    @Test
    fun rejectsUnsupportedMissingKeysAndOverlappingAttributes() {
        for ((name, type) in listOf("captainKey" to DynamoDbKeyScalarTypeDto.BOOLEAN,
            "" to DynamoDbKeyScalarTypeDto.STRING, "country" to DynamoDbKeyScalarTypeDto.STRING)) {
            val query = validQuery().apply { missingPrimaryKeyAttributes = linkedMapOf(name to type) }
            val randomness = Randomness()
            assertTrue(DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).isEmpty())
            assertEquals(Randomness().nextInt(), randomness.nextInt())
        }
    }

    /** Converter metadata must bootstrap empty tables and survive random mutation and copying. */
    @Test
    fun generatesValidKickoffInstantsWithoutObservedRows() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf("kickoff" to DynamoDbKeyScalarTypeDto.STRING)
            primaryKeyFormats = linkedMapOf("kickoff" to "date-time")
        }
        val randomness = Randomness()
        val gene = DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).single().attributes.last().gene
        assertTrue(gene is org.evomaster.core.search.gene.datetime.DateTimeGene)
        repeat(100) {
            java.time.Instant.parse(gene.getValueAsRawString())
            java.time.Instant.parse(gene.copy().getValueAsRawString())
            gene.randomize(randomness, false)
        }
        val injector = com.netflix.governator.guice.LifecycleInjector.builder()
            .withModules(org.evomaster.core.BaseModule(arrayOf("--blackBox", "false"), true))
            .build().createInjector()
        val config = injector.getInstance(org.evomaster.core.EMConfig::class.java)
        config.stoppingCriterion = org.evomaster.core.EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        config.maxEvaluations = 100
        val apc = injector.getInstance(org.evomaster.core.search.service.AdaptiveParameterControl::class.java)
        val mwc = injector.getInstance(org.evomaster.core.search.service.mutator.MutationWeightControl::class.java)
        gene.markAllAsInitialized()
        repeat(100) {
            gene.standardMutation(randomness, apc, mwc)
            java.time.Instant.parse(gene.getValueAsRawString())
            java.time.Instant.parse(gene.copy().getValueAsRawString())
        }
    }

    /** Unknown formats retain the ordinary string-key fallback. */
    @Test
    fun unknownConverterFormatDoesNotConstrainCountryKeys() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf("team" to DynamoDbKeyScalarTypeDto.STRING)
            primaryKeyFormats = linkedMapOf("team" to "custom")
        }
        assertTrue(DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single().attributes.last().gene is StringGene)
    }

    private fun validQuery(): DynamoDbFailedQuery = DynamoDbFailedQuery(
        "WorldCupPlayers",
        listOf(
            DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"),
            DynamoDbAttributeValueDto("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "10.50"),
            DynamoDbAttributeValueDto("captain", DynamoDbKeyScalarTypeDto.BOOLEAN, "true")
        )
    )
}
