package org.evomaster.e2etests.spring.openapi.v3.dictionarybase

import com.foo.rest.examples.spring.openapi.v3.dictionarybase.DictionaryBaseController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class DictionaryBaseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DictionaryBaseController())
        }
    }


    @Test
    fun testRunEM() {

        defaultSeed = 43

        runTestHandlingFlakyAndCompilation(
                "DictionaryBaseEM",
                100
        ) { args: MutableList<String> ->

            //White-box would trivially cover it via taint analysis
            setOption(args, "blackBox", "true")
            setOption(args, "base", baseUrlOfSut)
            setOption(args, "schema", "$baseUrlOfSut/v3/api-docs")
            setOption(args, "useDictionaryDataPool", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/dictionarybase/{aggregatortype}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/dictionarybase/{aggregatortype}", "OK")
        }
    }
}