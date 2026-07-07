package org.evomaster.e2etests.spring.rest.bb.jsonpatchdto

import com.foo.rest.examples.bb.jsonpatchdto.BBJsonPatchDtoController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.Solution
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.BlackBoxUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.nio.file.Files
import java.nio.file.Paths

class BBJsonPatchDtoTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBJsonPatchDtoController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource(value = OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun testJsonPatchDoesNotUseDtoWhenDtoPayloadsAreEnabled(outputFormat: OutputFormat) {
        val outputFolderName = "BBJsonPatchDtoEM"

        executeAndEvaluateBBTest(
            outputFormat,
            outputFolderName,
            1000,
            3,
            listOf(
                "JSON_PATCH_DTO_CREATED",
                "JSON_PATCH_DTO_CREATED_WITH_NAME",
                "JSON_PATCH_DTO_PATCHED"
            )
        ) { args: MutableList<String> ->

            setOption(args, "dtoForRequestPayload", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/jsonpatchdto/resources", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/jsonpatchdto/resources/{id}", "patched")
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 404, "/api/jsonpatchdto/resources/{id}", "missing")

            // assertHasAtLeastOne only checks each call exists somewhere in the solution, independently
            // and in any order. Here we additionally require a single generated test that first creates the
            // resource with a POST (201) and then manipulates it with a PATCH (200), which is what proves the
            // "DTO used for POST followed by a PATCH without DTO" scenario requested for this feature.
            assertPostThenPatchInSameTest(solution)
        }

        val generatedCode = readGeneratedCode(outputFormat, outputFolderName)
        assertTrue(
            generatedCode.contains("@JsonProperty(\"name\")")
                    && generatedCode.contains("@JsonProperty(\"age\")"),
            "POST request body should be emitted as a DTO with the Resource request fields"
        )
        assertFalse(
            generatedCode.contains("JsonPatchOperation"),
            "JSON Patch request body should keep using raw string JSON instead of a DTO"
        )
    }

    /**
     * Verifies that the solution contains at least one generated test (individual) in which a successful POST
     * that creates the resource (201) is followed, later in the same test, by a successful PATCH (200) on the
     * created resource. This is stronger than [assertHasAtLeastOne], which only checks each call exists somewhere
     * in the solution independently and in any order.
     */
    private fun assertPostThenPatchInSameTest(solution: Solution<RestIndividual>) {
        val postPath = RestPath("/api/jsonpatchdto/resources")
        val patchPath = RestPath("/api/jsonpatchdto/resources/{id}")

        val found = solution.individuals.any { ind ->
            val actions = ind.individual.seeMainExecutableActions()
            val results = ind.seeResults(actions)

            var createdIndex = -1
            for (i in actions.indices) {
                val action = actions[i]
                val result = results[i] as RestCallResult

                if (createdIndex < 0) {
                    if (action.verb == HttpVerb.POST
                        && result.getStatusCode() == 201
                        && action.path.isEquivalent(postPath)
                    ) {
                        createdIndex = i
                    }
                } else if (action.verb == HttpVerb.PATCH
                    && result.getStatusCode() == 200
                    && action.path.isEquivalent(patchPath)
                ) {
                    return@any true
                }
            }
            false
        }

        assertTrue(
            found,
            "Expected a single generated test where a POST (201) creating the resource is followed by a PATCH (200) on it"
        )
    }

    private fun readGeneratedCode(outputFormat: OutputFormat, outputFolderName: String): String {
        val baseLocation = if (outputFormat.isJava()) {
            BlackBoxUtils.baseLocationForJava
        } else {
            BlackBoxUtils.baseLocationForKotlin
        }

        val basePath = listOf(
            Paths.get(baseLocation),
            Paths.get("core-tests/e2e-tests/spring/spring-rest-bb", baseLocation)
        ).first { Files.exists(it) }

        return Files.walk(basePath)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().contains(outputFolderName) }
            .map { Files.readString(it) }
            .toList()
            .joinToString("\n")
    }
}