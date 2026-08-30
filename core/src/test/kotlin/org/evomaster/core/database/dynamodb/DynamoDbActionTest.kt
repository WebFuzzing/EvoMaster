package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests the DynamoDB initialization action and its execution result. */
class DynamoDbActionTest {

    @Test
    fun actionExposesStableMetadataAndCopiesGenesIndependently() {
        val original = DynamoDbAction(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeGene("country", DynamoDbScalarTypeDto.S, StringGene("country", "Argentina")))
        )

        val copy = original.copy() as DynamoDbAction
        (copy.attributes.single().gene as StringGene).value = "Brazil"

        assertEquals("DynamoDB_INSERT_WorldCupPlayers", original.getName())
        assertEquals(DynamoDbAction::class.java.name, original.getActionGroupKey())
        assertEquals("WorldCupPlayers|country:S=Argentina", original.insertionKey())
        assertSame(original.attributes.single().gene, original.seeTopGenes().single())
        assertNotSame(original.attributes.single().gene, copy.attributes.single().gene)
        assertEquals("Argentina", (original.attributes.single().gene as StringGene).value)
        assertEquals("Brazil", (copy.attributes.single().gene as StringGene).value)
    }

    @Test
    fun actionResultTracksInsertionOutcomeAndMatchesDynamoDbActions() {
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeGene("country", DynamoDbScalarTypeDto.S, StringGene("country", "Argentina")))
        )
        val result = DynamoDbActionResult("source")

        assertFalse(result.getInsertExecutionResult())
        result.setInsertExecutionResult(true)

        assertTrue(result.getInsertExecutionResult())
        assertTrue(result.matchedType(action))
        assertTrue(result.copy().getInsertExecutionResult())
    }

    @Test
    fun executionPreservesFailedQueriesAndAcceptsMissingDto() {
        val query = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.S, "Argentina"))
        )
        val dto = DynamoDbExecutionsDto()
        dto.failedQueries.add(query)

        assertSame(query, DynamoDbExecution.fromDto(dto).failedQueries.single())
        assertTrue(DynamoDbExecution.fromDto(null).failedQueries.isEmpty())
    }
}
