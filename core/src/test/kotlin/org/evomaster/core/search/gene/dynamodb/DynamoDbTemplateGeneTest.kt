package org.evomaster.core.search.gene.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.core.database.dynamodb.DynamoDbAction
import org.evomaster.core.database.dynamodb.DynamoDbAttributeGene
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DynamoDbTemplateGeneTest {

    @Test
    fun testNestedLeavesExtractedAndPromotedToChildGenes() {
        val payload = mapOf(
            "profile" to mapOf(
                "role" to "admin",
                "score" to 99.5,
                "active" to true
            ),
            "tags" to listOf("vip", "moderator")
        )
        val dto = DynamoDbAttributeValueDto.fromPlainValue("userData", payload)
        val templateGene = DynamoDbTemplateGene("userData", dto)

        assertTrue(templateGene.isMutable())
        assertEquals(5, templateGene.getViewOfChildren().size)

        val childMap = templateGene.getViewOfChildren().filterIsInstance<StringGene>().associateBy { it.name }
        assertTrue(childMap.containsKey("role"))
        assertEquals("admin", childMap["role"]!!.value)

        val numberGenes = templateGene.getViewOfChildren().filterIsInstance<BigDecimalGene>()
        assertEquals(1, numberGenes.size)
        assertEquals(BigDecimal("99.5"), numberGenes.first().value)
    }

    @Test
    fun testMutatingPromotedLeafUpdatesOutputDto() {
        val payload = mapOf(
            "profile" to mapOf(
                "role" to "admin",
                "department" to "engineering"
            )
        )
        val dto = DynamoDbAttributeValueDto.fromPlainValue("profileData", payload)
        val templateGene = DynamoDbTemplateGene("profileData", dto)

        val roleGene = templateGene.getViewOfChildren().filterIsInstance<StringGene>().first { it.name == "role" }
        roleGene.value = "viewer"

        val updatedDto = templateGene.toDto()
        val profileMap = updatedDto.mapValue["profile"]!!.mapValue
        assertEquals("viewer", profileMap["role"]!!.value)
        assertEquals("engineering", profileMap["department"]!!.value)
    }

    @Test
    fun testCopyPreservesStructureAndIsolatesMutations() {
        val payload = mapOf("status" to "PENDING", "step" to 1)
        val dto = DynamoDbAttributeValueDto.fromPlainValue("task", payload)
        val original = DynamoDbTemplateGene("task", dto)

        val copy = original.copy() as DynamoDbTemplateGene
        val copyStatusGene = copy.getViewOfChildren().filterIsInstance<StringGene>().first { it.name == "status" }
        copyStatusGene.value = "COMPLETED"

        assertEquals("COMPLETED", (copy.toDto().mapValue["status"])!!.value)
        assertEquals("PENDING", (original.toDto().mapValue["status"])!!.value)
    }

    @Test
    fun testNestedDocumentFieldBindsWithRestInput() {
        val payload = mapOf(
            "validationFlow" to mapOf(
                "eventId" to "EV_INIT_123",
                "status" to "QUEUED"
            )
        )
        val dto = DynamoDbAttributeValueDto.fromPlainValue("flow", payload)
        val templateGene = DynamoDbTemplateGene("flow", dto)

        val action = DynamoDbAction(
            "Workflows",
            listOf(DynamoDbAttributeGene("flow", dto.type, templateGene))
        )

        val inputEventId = StringGene("eventId", "EV_INIT_123")
        BindingBuilder.bindDynamoDbInsertActionsToInputs(listOf(action), listOf(inputEventId))

        val nestedEventId = templateGene.getViewOfChildren().filterIsInstance<StringGene>().first { it.name == "eventId" }
        assertTrue(inputEventId.isDirectBoundWith(nestedEventId))
        assertTrue(nestedEventId.isDirectBoundWith(inputEventId))

        // Mutating the REST input synchronizes into the nested document
        inputEventId.value = "EV_MUTATED_999"
        inputEventId.syncBindingGenesBasedOnThis()

        assertEquals("EV_MUTATED_999", nestedEventId.value)
        val updatedDto = templateGene.toDto()
        assertEquals("EV_MUTATED_999", updatedDto.mapValue["validationFlow"]!!.mapValue["eventId"]!!.value)
    }

    @Test
    fun testAdaptiveSelectSubsetToMutateIsImplemented() {
        val method = DynamoDbTemplateGene::class.java.getDeclaredMethod(
            "adaptiveSelectSubsetToMutate",
            Randomness::class.java,
            List::class.java,
            org.evomaster.core.search.service.mutator.MutationWeightControl::class.java,
            org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo::class.java
        )
        assertNotNull(method)
        assertEquals(DynamoDbTemplateGene::class.java, method.declaringClass)
    }
}
