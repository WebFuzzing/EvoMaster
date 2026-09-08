package org.evomaster.core.problem.asyncapi.data

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.core.EMConfig
import org.evomaster.core.database.mongo.MongoDbAction
import org.evomaster.core.problem.asyncapi.builder.AsyncApiActionBuilder
import org.evomaster.core.problem.asyncapi.builder.AsyncApiGeneBuilder
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.action.Action
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiIndividualTest {

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    /**
     * The six actions of the NCS document, in document order. Each is a fresh copy, so that a
     * test may put it in an individual without touching the template it came from.
     */
    private fun ncsActions(): List<AsyncApiAction> {
        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/sut/ncs-kafka.yaml")
        val cluster = mutableMapOf<String, Action>()
        AsyncApiActionBuilder.addActionsFromSchema(schema, cluster, AsyncApiGeneBuilder.options(EMConfig()))
        return cluster.values.map { it.copy() as AsyncApiAction }
    }


    @Test
    fun testAnIndividualHoldsTheMessagesToPublish() {

        val actions = ncsActions().take(2)

        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList())

        assertEquals(2, individual.seeMainExecutableActions().size)
        assertTrue(individual.canMutateStructure())
    }

    @Test
    fun testMessagesCanBeAddedAndRemoved() {

        val actions = ncsActions()

        val individual = AsyncApiIndividual(SampleType.RANDOM, mutableListOf(actions[0]))
        assertEquals(1, individual.seeMainExecutableActions().size)

        individual.addAction(action = actions[1])
        assertEquals(2, individual.seeMainExecutableActions().size)

        individual.removeAction(0)
        assertEquals(1, individual.seeMainExecutableActions().size)
        assertEquals(actions[1].getName(), individual.seeMainExecutableActions().first().getName())
    }

    @Test
    fun testCopyingAnIndividualKeepsItsMessages() {

        val actions = ncsActions().take(3)
        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList())

        val copy = individual.copy() as AsyncApiIndividual

        assertEquals(
            individual.seeMainExecutableActions().map { it.getName() },
            copy.seeMainExecutableActions().map { it.getName() }
        )
        //a copy must be independent, or mutating one would change the other
        assertNotSame(individual.seeMainExecutableActions()[0], copy.seeMainExecutableActions()[0])
    }

    @Test
    fun testCopyingAnIndividualThatWasSetUpWithMoreThanSql() {

        /*
            Only SQL is put in front of the messages today, but the individual inherits every
            other kind of setup an enterprise individual can hold. The children are copied
            wholesale, so a group whose size was not measured would not match what is handed
            over, and the copy would fail outright rather than come back wrong.
         */
        val actions = ncsActions().take(1)
        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList())

        individual.addInitializingMongoDbActions(
            actions = listOf(MongoDbAction("db", "collection", "collection", listOf()))
        )

        val copy = individual.copy() as AsyncApiIndividual

        assertEquals(1, copy.seeMainExecutableActions().size)
        assertEquals(
            individual.seeInitializingActions().size,
            copy.seeInitializingActions().size
        )
    }
}
