package org.evomaster.e2etests.spring.graphql.base


import com.foo.graphql.base.BaseWithOutschemaController
import org.evomaster.core.EMConfig
import org.evomaster.core.remote.SutProblemException
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class BaseWithOutSchemaTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseWithOutschemaController())
        }
    }



    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_BaseWithOutSchemaEM",
            "org.foo.graphql.BaseWithOutSchemaEM",
            20,
            false,
         { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            try {
                initAndRun(args)
                fail("Should had crashed")
            } catch (e: Exception){
                //expected, as introspective query is disabled
                assertEquals(SutProblemException::class.java, e.cause!!.javaClass)
            }
        },
                1)
    }
}