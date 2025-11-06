package org.evomaster.e2etests.spring.rest.bb.exampleobject

import com.foo.rest.examples.bb.exampleobject.BBExampleObjectController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBExampleObjectEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBExampleObjectController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "exampleobject",
            100,
            3,
            listOf("A")
        ){ args: MutableList<String> ->

            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbexampleobject.json")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbexampleobject", "OK")

            val bodies = solution.individuals
                .asSequence()
                .map { it.individual }
                .flatMap { it.seeMainExecutableActions() }
                .flatMap { it.parameters }
                .filterIsInstance<BodyParam>()
                .map { it.primaryGene().getValueAsRawString() }
                .toList()

            assertTrue(bodies.any { it.contains("Foo") }, "Missing Foo")
            assertTrue(bodies.any { it.contains("Y0") }, "Missing Y0")
            assertTrue(bodies.any { it.contains("Y1") }, "Missing Y1")
            assertTrue(bodies.any { it.contains("Y2") }, "Missing Y2")
            assertTrue(bodies.any { it.contains("Y3") }, "Missing Y3")
            assertTrue(bodies.any { it.contains("Y4") }, "Missing Y4")
        }
    }
}
