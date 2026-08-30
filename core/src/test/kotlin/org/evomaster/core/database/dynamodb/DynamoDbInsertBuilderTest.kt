package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests inference of DynamoDB initialization actions from failed reads. */
class DynamoDbInsertBuilderTest {

    @Test
    fun buildsTypedActionFromSupportedFailedQuery() {
        val actions = DynamoDbInsertBuilder.buildInsertActions(listOf(validQuery()), emptySet())

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
            listOf(DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.N, "ten"))
        )
        val blankTable = DynamoDbFailedQuery(
            "",
            listOf(DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.S, "Argentina"))
        )
        val missingType = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto("country", null, "Argentina"))
        )

        val actions = DynamoDbInsertBuilder.buildInsertActions(
            listOf(valid, valid, invalidNumber, blankTable, missingType),
            emptySet()
        )

        assertEquals(1, actions.size)
        assertTrue(
            DynamoDbInsertBuilder.buildInsertActions(listOf(valid), setOf(actions.single().insertionKey())).isEmpty()
        )
    }

    private fun validQuery(): DynamoDbFailedQuery = DynamoDbFailedQuery(
        "WorldCupPlayers",
        listOf(
            DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.S, "Argentina"),
            DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.N, "10.50"),
            DynamoDbAttributeValueDto("captain", DynamoDbScalarTypeDto.BOOL, "true")
        )
    )
}
