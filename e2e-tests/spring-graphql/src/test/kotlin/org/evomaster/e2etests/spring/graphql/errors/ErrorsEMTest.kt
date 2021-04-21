package org.evomaster.e2etests.spring.graphql.errors

import com.foo.graphql.errors.ErrorsController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class ErrorsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ErrorsController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_ErrorsEM",
            "org.foo.graphql.ErrorsEM",
            100
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            args.add("--exportCoveredTarget")
            args.add("true")

            val targetFile = "target/covered-targets/errors.txt"
            args.add("--coveredTargetFile")
            args.add(targetFile)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertAnyWithErrors(solution)

            existErrorAndSuccessTarget(targetFile)

        }
    }

    private fun existErrorAndSuccessTarget(path : String){
        val file = File(path)
        assertTrue(file.exists())

        val targets = file.readText()
        assertTrue(targets.contains("GQL_ERRORS:") && targets.contains("GQL_NO_ERRORS:"), targets)
    }


}