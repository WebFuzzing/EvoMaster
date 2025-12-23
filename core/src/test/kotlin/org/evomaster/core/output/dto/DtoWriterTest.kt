package org.evomaster.core.output.dto

import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getBodyParam
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections.singletonList

class DtoWriterTest {

    companion object {
        val outputTestSuitePath: Path = Paths.get("./target/dto-writer-test")
        val outputFormat = OutputFormat.JAVA_JUNIT_4

        val config = EMConfig().apply {
            aiModelForResponseClassification = EMConfig.AIResponseClassifierModel.GLM
            enableSchemaConstraintHandling = true
            allowInvalidData = false
            probRestDefault = 0.0
            probRestExamples = 0.0
        }
        val options = RestActionBuilderV3.Options(config)
        const val STRING = "String"
        const val INTEGER = "Integer"
        const val BOOLEAN = "Boolean"

        const val TEST_PACKAGE = "test.package"

        // Since we changed the method signature for [DtoWriter.write], we're using a mock solution for tests to
        // compile. These tests are currently ignored until they're refactored into integration tests using
        // reflection to assert on the DTOs being correctly instantiated and created.
        val restAction = getRestCallAction("/items", HttpVerb.POST,  mutableListOf(getBodyParam()))
        val eIndividual = getEvaluatedIndividualWith(restAction)
        val MOCK_SOLUTION = Solution(singletonList(eIndividual), "", "", Termination.NONE, emptyList(), emptyList())
    }

    @Test
    fun javaAndKotlinAreOnlySupportedForDtos() {
        val supportedOutputFormats = listOf(OutputFormat.JAVA_JUNIT_4, OutputFormat.JAVA_JUNIT_5,
            OutputFormat.KOTLIN_JUNIT_4, OutputFormat.KOTLIN_JUNIT_5)
        val unsupportedOutputFormats = listOf(OutputFormat.JS_JEST, OutputFormat.PYTHON_UNITTEST)

        supportedOutputFormats.forEach { outputFormat ->
            val dtoWriter = DtoWriter(outputFormat)
            dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)
            assertTrue(dtoWriter.getCollectedDtos().isNotEmpty())
        }

        unsupportedOutputFormats.forEach { outputFormat ->
            assertThrows(IllegalStateException::class.java, {
                val dtoWriter = DtoWriter(outputFormat)
                dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)
            })
        }
    }

    @Test
    fun emptySolutionListReturnsNoDtos() {
        val dtoWriter = DtoWriter(outputFormat)
        val emptySolution = Solution<RestIndividual>(mutableListOf(), "", "", Termination.NONE, emptyList(), emptyList())

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, emptySolution)

        assertTrue(dtoWriter.getCollectedDtos().isEmpty())
    }

    @Test
    fun noDtosWhenNoBodyParam() {
        val dtoWriter = DtoWriter(outputFormat)
        val eIndividual = getEvaluatedIndividualWith(getRestCallAction("/items", HttpVerb.POST))
        val noBodyParamSolution = Solution(singletonList(eIndividual), "", "", Termination.NONE, emptyList(), emptyList())

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, noBodyParamSolution)

        assertEquals(dtoWriter.getCollectedDtos().size, 0)
    }

    // TODO: Migrate tests to integration tests using reflection to assert correct DTO generation
    @Disabled("Tests disabled until migrated to integration tests")
    @Test
    fun testOneOfMergesDtosIntoASingleOne() {
        val dtoSpecs = listOf("Components", "Inline", "Mixed")
        dtoSpecs.forEach { chosenDto ->
            val dtoWriter = DtoWriter(outputFormat)
            val actionCluster = initRestSchema("choice/oneOf$chosenDto.yaml")
            dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)
            val collectedDtos = dtoWriter.getCollectedDtos()
            assertEquals(collectedDtos.size, 1)
            val oneOfDto = collectedDtos[collectedDtos.keys.first()]
            assertNotNull(oneOfDto)
//            val dtoFields = oneOfDto?.fields?:emptyList()
            val dtoFields = oneOfDto?.fieldsMap?:emptyMap()
            assertEquals(dtoFields.size, 2)
            assertDtoFieldIn(dtoFields, "dog", STRING)
            assertDtoFieldIn(dtoFields, "cat", STRING)
        }
    }

    @Disabled("Tests disabled until migrated to integration tests")
    @Test
    fun testAnyOfArrayAndObject() {
        val chosenDto = "ArrayAndObject"
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("choice/anyOf$chosenDto.yaml")
        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)
        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 1)
        val anyOfDto = collectedDtos[collectedDtos.keys.first()]
        assertNotNull(anyOfDto)
//        val dtoFields = anyOfDto?.fields?:emptyList()
        val dtoFields = anyOfDto?.fieldsMap?:emptyMap()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "email", STRING)
        assertDtoFieldIn(dtoFields, "numbers", "List<Integer>")
    }

    private fun initRestSchema(openApiLocation: String) : Map<String, Action> {
        val restSchema = RestSchema(OpenApiAccess.getOpenAPIFromResource("/swagger/dto-writer/$openApiLocation"))
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)
        return actionCluster
    }

    private fun assertDtoFieldIn(dtoFields: Map<String, DtoField>, targetName: String, targetType: String) {
        assertThat(dtoFields[targetName], `is`(DtoField(targetName, targetType)))
    }

    private fun assertDtoFieldIn(dtoFields: List<DtoField>, targetName: String, targetType: String) {
        assertThat(dtoFields, hasItem(DtoField(targetName, targetType)))
    }
}
