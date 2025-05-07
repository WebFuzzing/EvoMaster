package org.evomaster.e2etests.spring.openapi.v3.boottimetargets

import com.foo.rest.examples.spring.openapi.v3.boottimetargets.BootTimeTargetsController
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDateTime

class BootTimeTargetsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BootTimeTargetsController())
        }
    }

    private val targetFile = "target/covered-targets/boottimetargets.txt"

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "BootTimeTargetsEMTest",
                "org.foo.BootTimeTargetsEMTest",
                20
        ) { args: MutableList<String> ->

            args.add("--exportCoveredTarget")
            args.add("true")
            args.add("--coveredTargetFile")
            args.add(targetFile)

            // disable the assertion since the response is flaky with LocalDateTime.now
            args.add("--enableBasicAssertions")
            args.add("false")

            val solution = initAndRun(args)

            val done = LocalDateTime.now()
            assertTrue(solution.individuals.size >= 1)

            val ok = solution.individuals.all { e-> e.evaluatedMainActions().filter {
                // skip call to get swagger
                it.action.getName().startsWith("/api")
            }.all { r-> (r.result as? RestCallResult)?.getBody()?.split(";")?.run {
                LocalDateTime.parse(this[0]).isBefore(done) && LocalDateTime.parse(this[0]).isBefore(LocalDateTime.parse(this[1]))
            }?:false } }
            assertTrue(ok)

            existBootTimeTarget()
        }
    }

    private fun existBootTimeTarget(){
        val file = File(targetFile)
        assertTrue(file.exists())

        val targets = file.readText()
        assertTrue(targets.contains("Class_com.foo.rest.examples.spring.openapi.v3.SpringController") &&
                targets.contains("Class_com.foo.rest.examples.spring.openapi.v3.boottimetargets.BootTimeTargetsApplication") &&
                targets.contains("Line_at_com.foo.rest.examples.spring.openapi.v3.boottimetargets.BootTimeTargetsRest_00014") &&
                targets.contains("Branch_at_com.foo.rest.examples.spring.openapi.v3.boottimetargets.BootTimeTargetsRest_at_line_00040_position_0_falseBranch_154"), targets)
    }

}