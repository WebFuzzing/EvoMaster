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

    @Test
    fun testCompareTwoStatusCodes() {

        val testCode1 = "201"
        val testCode2 = "201"

        Assert.assertTrue(RestIndividualSelectorUtils.compareTwoStatusCodes(testCode1, testCode2))

        val testCode3 = "204"
        val testCode4 = "205"

        Assert.assertFalse(RestIndividualSelectorUtils.compareTwoStatusCodes(testCode3, testCode4))

        val testCode5 = "201"
        val testCode6 = "2x1"

        Assert.assertTrue(RestIndividualSelectorUtils.compareTwoStatusCodes(testCode5, testCode6))

        val testCode7 = "20X"
        val testCode8 = "207"

        Assert.assertTrue(RestIndividualSelectorUtils.compareTwoStatusCodes(testCode7, testCode8))

        val testCode9 = "2XX"
        val testCode10 = "2X5"

        Assert.assertTrue(RestIndividualSelectorUtils.compareTwoStatusCodes(testCode9, testCode10))

    }

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