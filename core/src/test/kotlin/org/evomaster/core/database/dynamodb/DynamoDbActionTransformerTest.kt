package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
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
                DynamoDbAttributeGene("country", DynamoDbValueType.STRING, StringGene("country", "Argentina")),
                DynamoDbAttributeGene("fifaId", DynamoDbValueType.NUMBER, BigDecimalGene("fifaId", BigDecimal("10.50"))),
                DynamoDbAttributeGene("captain", DynamoDbValueType.BOOLEAN, BooleanGene("captain", true))
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
    fun transformsDocumentSetAndNullAttributes() {
        val rolesDto = DynamoDbDtoUtils.fromPlainValue("roles", setOf("forward", "captain"))
        val scoresDto = DynamoDbDtoUtils.fromPlainValue("scores", setOf(BigDecimal("10.50"), BigDecimal("2")))
        val nestedDto = DynamoDbDtoUtils.fromPlainValue(
            "details",
            mapOf("tags" to setOf("world-cup", "mvp"), "coach" to null)
        )
        val nullDto = DynamoDbDtoUtils.fromPlainValue("retiredAt", null)

        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene("roles", DynamoDbValueType.STRING_SET, DynamoDbInsertBuilder.buildDocumentGene("roles", rolesDto)),
                DynamoDbAttributeGene("scores", DynamoDbValueType.NUMBER_SET, DynamoDbInsertBuilder.buildDocumentGene("scores", scoresDto)),
                DynamoDbAttributeGene("details", DynamoDbValueType.MAP, DynamoDbInsertBuilder.buildDocumentGene("details", nestedDto)),
                DynamoDbAttributeGene("retiredAt", DynamoDbValueType.NULL, DynamoDbInsertBuilder.buildDocumentGene("retiredAt", nullDto))
            )
        )

        val attributes = DynamoDbActionTransformer.transform(listOf(action)).insertions.single().attributes
        assertEquals(DynamoDbValueTypeDto.STRING_SET, attributes[0].type)
        assertEquals(listOf("captain", "forward"), attributes[0].setValue)
        assertEquals(DynamoDbValueTypeDto.NUMBER_SET, attributes[1].type)
        assertEquals(listOf("2", "10.50"), attributes[1].setValue)
        assertEquals(DynamoDbValueTypeDto.MAP, attributes[2].type)
        assertEquals(DynamoDbValueTypeDto.STRING_SET, attributes[2].mapValue.getValue("tags").type)
        assertEquals(listOf("mvp", "world-cup"), attributes[2].mapValue.getValue("tags").setValue)
        assertEquals(DynamoDbValueTypeDto.NULL, attributes[2].mapValue.getValue("coach").type)
        assertEquals(DynamoDbValueTypeDto.NULL, attributes[3].type)
    }

    @Test
    fun transformsAnEmptyActionList() {
        assertTrue(DynamoDbActionTransformer.transform(emptyList()).insertions.isEmpty())
    }

    /** Copies of individuals and insertion DTOs must not share document payloads. */
    @Test
    fun isolatesNestedWorldCupTemplates() {
        val source = DynamoDbDtoUtils.fromPlainValue("squad", mapOf("players" to listOf("Lionel Messi")))
        val docGene = DynamoDbInsertBuilder.buildDocumentGene("squad", source)
        val action = DynamoDbAction("WorldCupPlayers", listOf(DynamoDbAttributeGene("squad", DynamoDbValueType.MAP, docGene)))
        source.mapValue.clear()
        val copied = action.copy() as DynamoDbAction
        val transported = DynamoDbActionTransformer.transform(listOf(action)).insertions.single().attributes.single()
        transported.mapValue.clear()
        val independent = DynamoDbActionTransformer.transform(listOf(copied)).insertions.single().attributes.single()
        assertEquals("Lionel Messi", independent.mapValue.getValue("players").listValue.single().value)
    }
}
