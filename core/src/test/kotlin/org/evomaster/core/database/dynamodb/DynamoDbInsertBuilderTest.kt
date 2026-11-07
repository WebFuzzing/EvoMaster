package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
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
    fun skipsInvalidQueriesAndBuildsAnActionForEachReportedRead() {
        val valid = validQuery()
        val invalidNumber = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.NUMBER, "ten"))
        )
        val blankTable = DynamoDbFailedQuery(
            "",
            listOf(DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.STRING, "Argentina"))
        )
        val missingType = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto("country", null, "Argentina"))
        )

        val actions = DynamoDbInsertBuilder.buildInsertActions(
            listOf(valid, valid, invalidNumber, blankTable, missingType),
            Randomness()
        )

        assertEquals(2, actions.size)
    }

    @Test
    fun generatesReproducibleWorldCupKeysWithoutRandomizingInferredValues() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf(
                "recordKey" to DynamoDbScalarTypeDto.STRING,
                "matchNumber" to DynamoDbScalarTypeDto.NUMBER
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
            missingPrimaryKeyAttributes = linkedMapOf("recordKey" to DynamoDbScalarTypeDto.STRING)
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
        for ((name, type) in listOf("captainKey" to DynamoDbScalarTypeDto.BOOLEAN,
            "" to DynamoDbScalarTypeDto.STRING, "country" to DynamoDbScalarTypeDto.STRING)) {
            val query = validQuery().apply { missingPrimaryKeyAttributes = linkedMapOf(name to type) }
            val randomness = Randomness()
            assertTrue(DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).isEmpty())
            assertEquals(Randomness().nextInt(), randomness.nextInt())
        }
    }

    private fun validQuery(): DynamoDbFailedQuery = DynamoDbFailedQuery(
        "WorldCupPlayers",
        listOf(
            DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.STRING, "Argentina"),
            DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.NUMBER, "10.50"),
            DynamoDbAttributeValueDto("captain", DynamoDbScalarTypeDto.BOOLEAN, "true")
        )
    )
}
