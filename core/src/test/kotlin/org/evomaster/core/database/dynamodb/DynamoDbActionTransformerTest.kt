package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.dynamodb.DynamoDbTemplateGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Tests conversion of DynamoDB initialization actions to controller DTOs. */
class DynamoDbActionTransformerTest {

    @Test
    fun transformsAllSupportedScalarTypes() {
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene("country", DynamoDbKeyScalarTypeDto.STRING, StringGene("country", "Argentina")),
                DynamoDbAttributeGene("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, BigDecimalGene("fifaId", BigDecimal("10.50"))),
                DynamoDbAttributeGene("captain", DynamoDbKeyScalarTypeDto.BOOLEAN, BooleanGene("captain", true))
            )
        )

        val insertion = DynamoDbActionTransformer.transform(listOf(action)).insertions.single()

        assertEquals("WorldCupPlayers", insertion.tableName)
        assertEquals("Argentina", insertion.attributes[0].value)
        assertEquals("10.50", insertion.attributes[1].value)
        assertEquals("true", insertion.attributes[2].value)
        assertEquals(
            listOf(DynamoDbValueTypeDto.STRING, DynamoDbValueTypeDto.NUMBER, DynamoDbValueTypeDto.BOOLEAN),
            insertion.attributes.map { it.type }
        )
    }

    @Test
    fun transformsAnEmptyActionList() {
        assertTrue(DynamoDbActionTransformer.transform(emptyList()).insertions.isEmpty())
    }

    /** Copies of individuals and insertion DTOs must not share document payloads. */
    @Test
    fun isolatesNestedWorldCupTemplates() {
        val source = DynamoDbAttributeValueDto.fromPlainValue("squad", mapOf("players" to listOf("Lionel Messi")))
        val action = DynamoDbAction("WorldCupPlayers", listOf(DynamoDbAttributeGene("squad", source.type,
            DynamoDbTemplateGene("squad", source))))
        source.mapValue.clear()
        val copied = action.copy() as DynamoDbAction
        val transported = DynamoDbActionTransformer.transform(listOf(action)).insertions.single().attributes.single()
        transported.mapValue.clear()
        val independent = DynamoDbActionTransformer.transform(listOf(copied)).insertions.single().attributes.single()
        assertEquals("Lionel Messi", independent.mapValue.getValue("players").listValue.single().value)
    }
}
