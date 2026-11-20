package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.dynamodb.DynamoDbSetGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Tests the DynamoDB initialization action and its execution result. */
class DynamoDbActionTest {

    @Test
    fun actionExposesStableMetadataAndCopiesGenesIndependently() {
        val original = DynamoDbAction(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeGene("country", DynamoDbValueType.STRING, StringGene("country", "Argentina")))
        )

        val copy = original.copy() as DynamoDbAction
        (copy.attributes.single().gene as StringGene).value = "Brazil"

        assertEquals("DynamoDB_INSERT_WorldCupPlayers", original.getName())
        assertEquals(DynamoDbAction::class.java.name, original.getActionGroupKey())
        assertEquals("WorldCupPlayers|country:STRING=Argentina", original.insertionKey())
        assertSame(original.attributes.single().gene, original.seeTopGenes().single())
        assertNotSame(original.attributes.single().gene, copy.attributes.single().gene)
        assertEquals("Argentina", (original.attributes.single().gene as StringGene).value)
        assertEquals("Brazil", (copy.attributes.single().gene as StringGene).value)
    }

    @Test
    fun actionComputesInsertionKeyForDocumentAndSetAttributesWithoutThrowing() {
        val rolesDto = DynamoDbDtoUtils.fromPlainValue("roles", setOf("captain", "forward"))
        val scoresDto = DynamoDbDtoUtils.fromPlainValue("scores", setOf(BigDecimal("10.50"), BigDecimal("2")))
        val action = DynamoDbAction(
            "Users",
            listOf(
                DynamoDbAttributeGene("id", DynamoDbValueType.STRING, StringGene("id", "user-1")),
                DynamoDbAttributeGene("details", DynamoDbValueType.MAP, ObjectGene("details", listOf(StringGene("city", "Rome")))),
                DynamoDbAttributeGene("roles", DynamoDbValueType.STRING_SET, DynamoDbInsertBuilder.buildDocumentGene("roles", rolesDto)),
                DynamoDbAttributeGene("scores", DynamoDbValueType.NUMBER_SET, DynamoDbInsertBuilder.buildDocumentGene("scores", scoresDto))
            )
        )
        val key = action.insertionKey()
        assertTrue(key.startsWith("Users|id:STRING=user-1|details:MAP={"))
        assertTrue(key.contains("|roles:STRING_SET={"))
        assertTrue(key.contains("|scores:NUMBER_SET={"))
    }

    @Test
    fun setGenesPreserveTypesAndCopyIndependently() {
        val ssGene = DynamoDbSetGene(
            "positions",
            DynamoDbValueType.STRING_SET,
            listOf(StringGene("positions", "forward"), StringGene("positions", "captain"))
        )
        val nsGene = DynamoDbSetGene(
            "numbers",
            DynamoDbValueType.NUMBER_SET,
            listOf(BigDecimalGene("numbers", BigDecimal("10.50")), BigDecimalGene("numbers", BigDecimal("2")))
        )
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene("positions", DynamoDbValueType.STRING_SET, ssGene),
                DynamoDbAttributeGene("numbers", DynamoDbValueType.NUMBER_SET, nsGene)
            )
        )

        val copy = action.copy() as DynamoDbAction
        ((copy.attributes[0].gene as DynamoDbSetGene).elements[0] as StringGene).value = "goalkeeper"
        ((copy.attributes[1].gene as DynamoDbSetGene).elements[0] as BigDecimalGene).value = BigDecimal("99")

        assertEquals(setOf("captain", "forward"), action.attributes[0].toPlainValue())
        assertEquals(setOf("captain", "goalkeeper"), copy.attributes[0].toPlainValue())
        assertEquals(listOf("captain", "forward"), action.attributes[0].toDto().setValue)
        assertEquals(DynamoDbValueTypeDto.STRING_SET, action.attributes[0].toDto().type)
        assertEquals(listOf("2", "10.50"), action.attributes[1].toDto().setValue)
        assertEquals(listOf("2", "99"), copy.attributes[1].toDto().setValue)
    }

    @Test
    fun actionResultTracksInsertionOutcomeAndMatchesDynamoDbActions() {
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeGene("country", DynamoDbValueType.STRING, StringGene("country", "Argentina")))
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
            listOf(DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"))
        )
        val dto = DynamoDbExecutionsDto()
        dto.failedQueries.add(query)

        assertSame(query, DynamoDbExecution.fromDto(dto).failedQueries.single())
        assertTrue(DynamoDbExecution.fromDto(null).failedQueries.isEmpty())
    }
}
