package org.evomaster.core.problem.rest

import org.evomaster.core.TestUtils
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.junit.Assert
import org.junit.jupiter.api.Test

/**
 * This class is to test methods inside RestIndividualSelectorUtils.kt
 */
class RestIndividualSelectorUtilsTest {



    /**
     * This test case is written to test the method findIndividualsContainingActionsWithGivenParameters
     * where only the verb is given
     *
     * We create two individuals, one containing one GET request, another containing one POST request
     * If we select the individual containing only GET request, we should get the first one only.
     *
     */
    @Test
    fun testIndividualSelectionBasedOnVerb() {

        val action1 = TestUtils.generateFakeRestActionWithVerb("1", HttpVerb.GET, "/path1")
        val action2 = TestUtils.generateFakeRestActionWithVerb("2", HttpVerb.POST, "/path2")

        val fakeIndividual1 = RestIndividual(
            mutableListOf(
                RestResourceCalls(actions = listOf(action1), sqlActions = listOf()),
            ),
            SampleType.RANDOM
        )

        val fakeIndividual2 = RestIndividual(
            mutableListOf(
                RestResourceCalls(actions = listOf(action2), sqlActions = listOf()),
            ),
            SampleType.RANDOM
        )

        // TODO Make those evaluated individual to test methods
        fakeIndividual1.ensureFlattenedStructure()
        fakeIndividual2.ensureFlattenedStructure()



    }


}