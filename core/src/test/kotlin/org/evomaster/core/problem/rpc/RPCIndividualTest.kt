package org.evomaster.core.problem.rpc

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.sql.SqlAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RPCIndividualTest {

    private fun call(id: String) = RPCCallAction(
        interfaceId = "com.foo.Service",
        id = id,
        inputParameters = mutableListOf<Param>(RPCParam("arg", IntegerGene("arg"))),
        responseTemplate = null,
        response = null
    )

    @Test
    fun testSeeIndexedRPCCalls() {

        val actions = mutableListOf(call("a"), call("b"), call("c"))

        val individual = RPCIndividual(SampleType.RANDOM, actions = actions)

        /*
            The calls are not children of the individual directly: each is wrapped in an
            EnterpriseActionGroup. Looking them up by action type therefore finds nothing unless
            the group is unwrapped.
         */
        val indexed = individual.seeIndexedRPCCalls()

        assertEquals(3, indexed.size)
        assertEquals(listOf("a", "b", "c"), indexed.entries.sortedBy { it.key }.map { it.value.getName() })
    }

    @Test
    fun testIndicesAreThoseOfTheChildrenHoldingTheCalls() {

        val actions = mutableListOf(call("a"), call("b"))

        val individual = RPCIndividual(SampleType.RANDOM, actions = actions)

        individual.seeIndexedRPCCalls().forEach { (index, action) ->
            //the index must address the child that holds the call, not some other numbering
            assertSame(action, (individual.getViewOfChildren()[index] as Any).let {
                (it as org.evomaster.core.problem.enterprise.EnterpriseActionGroup<*>).getMainAction()
            })
        }
    }

    @Test
    fun testCallsAreFoundPastInitializingActions() {

        val actions = mutableListOf(call("a"))

        val individual = RPCIndividual(
            SampleType.RANDOM,
            actions = actions,
            dbInitialization = mutableListOf<SqlAction>()
        )

        assertEquals(1, individual.seeIndexedRPCCalls().size)
        assertEquals("a", individual.seeIndexedRPCCalls().values.first().getName())
    }

    @Test
    fun testAnIndividualWithNoCalls() {
        assertTrue(RPCIndividual(SampleType.RANDOM, actions = mutableListOf()).seeIndexedRPCCalls().isEmpty())
    }
}
