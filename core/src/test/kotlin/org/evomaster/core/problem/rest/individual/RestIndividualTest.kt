package org.evomaster.core.problem.rest.individual

import org.evomaster.core.TestUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * created by manzhang on 2024/1/30
 */
class RestIndividualTest {

    @Test
    fun testFlattenStructure(){
        val fakeInd = TestUtils.generateFakeSimpleRestIndividual()
        assertTrue(fakeInd.getResourceCalls().isNotEmpty())
        fakeInd.ensureFlattenedStructure()

    }
}