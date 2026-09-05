package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
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
                DynamoDbAttributeGene("country", DynamoDbScalarTypeDto.STRING, StringGene("country", "Argentina")),
                DynamoDbAttributeGene("fifaId", DynamoDbScalarTypeDto.NUMBER, BigDecimalGene("fifaId", BigDecimal("10.50"))),
                DynamoDbAttributeGene("captain", DynamoDbScalarTypeDto.BOOLEAN, BooleanGene("captain", true))
            )
        )

        val insertion = DynamoDbActionTransformer.transform(listOf(action)).insertions.single()

        assertEquals("WorldCupPlayers", insertion.tableName)
        assertEquals("Argentina", insertion.attributes[0].value)
        assertEquals("10.50", insertion.attributes[1].value)
        assertEquals("true", insertion.attributes[2].value)
        assertEquals(
            listOf(DynamoDbScalarTypeDto.STRING, DynamoDbScalarTypeDto.NUMBER, DynamoDbScalarTypeDto.BOOLEAN),
            insertion.attributes.map { it.type }
        )
    }

    @Test
    fun transformsAnEmptyActionList() {
        assertTrue(DynamoDbActionTransformer.transform(emptyList()).insertions.isEmpty())
    }
}
