package org.evomaster.e2etests.spring.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2

import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.FunctionInReturnedObjectWithPrimitivesAndObjectsAsInputController2
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FunctionInReturnedObjectWithPrimitivesAndObjectsAsInputTest2 : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FunctionInReturnedObjectWithPrimitivesAndObjectsAsInputController2())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_FunctionInReturnedObjectWithPrimitivesAndObjectsAsInput2EM",
            "org.foo.graphql.FunctionInReturnedObjectWithPrimitivesAndObjectsAsInput2EM",
            100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertNoneWithErrors(solution)
            assertValueInDataAtLeastOnce(solution,"NON_NULL_STORE")
            assertValueInDataAtLeastOnce(solution,"NULL_ID")
        }
    }

}