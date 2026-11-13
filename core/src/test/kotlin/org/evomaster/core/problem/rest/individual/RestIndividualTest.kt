package org.evomaster.core.problem.rest.individual

import org.evomaster.core.TestUtils
import org.evomaster.core.database.dynamodb.DynamoDbAction
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * created by manzhang on 2024/1/30
 */
class RestIndividualTest {

    @Test
    fun testDynamoDbInsertionFollowsTheRestInput() {
        val rest = TestUtils.generateFakeQueryRestAction("1", "/players")
        val country = rest.seeTopGenes().filterIsInstance<StringGene>().single()
        country.value = "Argentina"
        val individual = RestIndividual(mutableListOf(rest), SampleType.RANDOM)
        individual.resetLocalIdRecursively()
        individual.doInitializeLocalId()
        individual.doInitialize()

        val insertion = TestUtils.generateFakeDynamoDbAction("WorldCupPlayers", "country", "Argentina")
        individual.addInitializingActions(listOf(insertion))
        BindingBuilder.bindDynamoDbInsertActionsToInputs(
            listOf(insertion), individual.seeFullTreeGenes(ActionFilter.NO_INIT)
        )
        assertTrue(individual.verifyBindingGenes())

        val copy = individual.copy() as RestIndividual
        val copiedCountry = copy.seeFullTreeGenes(ActionFilter.NO_INIT).filterIsInstance<StringGene>().single()
        val copiedInsertion = copy.seeDynamoDbActions().single() as DynamoDbAction
        copiedCountry.value = "Uruguay"
        copiedCountry.syncBindingGenesBasedOnThis()

        assertEquals("Uruguay", (copiedInsertion.attributes.single().gene as StringGene).value)
        assertEquals("Argentina", (insertion.attributes.single().gene as StringGene).value)
    }



    @Test
    fun testFlattenStructure(){
        val twoDbActions = TestUtils.generateTwoFakeDbActions(1001L, 1002L, 12345L, 10L, "Foo", "Bar", 0, 42)
        val fooAction = TestUtils.generateFakeQueryRestAction("1", "/foo")
        val barAction = TestUtils.generateFakeQueryRestAction("2", "/bar", true)
        val dynamoDbAction = TestUtils.generateFakeDynamoDbAction("WorldCupPlayers", "country", "Argentina")
        val fooResource = RestResourceCalls(
            actions = listOf(fooAction),
            sqlActions = listOf(twoDbActions[0])
        ).apply { addChild(dynamoDbAction) }
        assertEquals(listOf(dynamoDbAction), fooResource.seeActions(ActionFilter.ONLY_DYNAMODB))
        assertTrue(fooResource.seeActions(ActionFilter.INIT).contains(dynamoDbAction))
        assertTrue(fooResource.seeActions(ActionFilter.ONLY_DB).contains(dynamoDbAction))

        val fakeInd = RestIndividual(
            mutableListOf(
                fooResource,
                RestResourceCalls(actions = listOf(barAction), sqlActions = listOf(twoDbActions[1]))
            ),
            SampleType.RANDOM
        )

        assertTrue(fakeInd.getResourceCalls().isNotEmpty())
        assertEquals(2, fakeInd.getResourceCalls().size)
        assertTrue(fakeInd.seeInitializingActions().isEmpty())

        fakeInd.resetLocalIdRecursively()
        fakeInd.doInitializeLocalId()
        fakeInd.doInitialize()

        fakeInd.ensureFlattenedStructure()

        assertTrue(fakeInd.getResourceCalls().isEmpty())
        assertEquals(3, fakeInd.seeInitializingActions().size)
        assertEquals(listOf(dynamoDbAction), fakeInd.seeDynamoDbActions())
        assertEquals(2, fakeInd.seeMainExecutableActions().size)

    }
}
