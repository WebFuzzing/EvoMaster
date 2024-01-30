package org.evomaster.core.problem.rest.individual

import org.evomaster.core.TestUtils
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * created by manzhang on 2024/1/30
 */
class RestIndividualTest {



    @Test
    fun testFlattenStructure(){
        val twoDbActions = TestUtils.generateTwoFakeDbActions(1001L, 1002L, 12345L, 10L, "Foo", "Bar", 0, 42)
        val fooAction = TestUtils.generateFakeQueryRestAction("1", "/foo")
        val barAction = TestUtils.generateFakeQueryRestAction("2", "/bar", true)

        val fakeInd = RestIndividual(
            mutableListOf(
                RestResourceCalls(actions = listOf(fooAction), sqlActions = listOf(twoDbActions[0])),
                RestResourceCalls(actions = listOf(barAction), sqlActions = listOf(twoDbActions[1]))
            ),
            SampleType.RANDOM
        )

        assertTrue(fakeInd.getResourceCalls().isNotEmpty())
        assertEquals(2, fakeInd.getResourceCalls().size)
        assertTrue(fakeInd.seeInitializingActions().isEmpty())

        fakeInd.ensureFlattenedStructure()

        assertTrue(fakeInd.getResourceCalls().isEmpty())
        assertEquals(2, fakeInd.seeInitializingActions().size)
        assertEquals(2, fakeInd.seeMainExecutableActions().size)

    }
}