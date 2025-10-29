package org.evomaster.core.output.dto

import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

// TODO: Migrate tests to integration tests using reflection to assert correct DTO generation
@Disabled("Tests disabled until migrated to integration tests")
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
        val MOCK_SOLUTION = Solution(mutableListOf<EvaluatedIndividual<RestIndividual>>(), "", "", Termination.NONE, emptyList(), emptyList())
    }

    @Test
    fun javaAndKotlinAreOnlySupportedForDtos() {
        val actionCluster = initRestSchema("primitiveTypes.yaml")
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
    fun emptyActionListReturnsNoDtos() {
        val dtoWriter = DtoWriter(outputFormat)

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        assertTrue(dtoWriter.getCollectedDtos().isEmpty())
    }


    @Test
    fun arrayAsRootTypeCollectsASingleDto() {
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("array/rootArrayWithComponents.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 1)
        val userDto = collectedDtos["User"]
        assertNotNull(userDto)
        val dtoFields = userDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "name", STRING)
        assertDtoFieldIn(dtoFields, "age", INTEGER)
    }

    @Test
    fun arrayOfInlineObjectUsesPropertyName() {
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("array/arrayOfInlineObject.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 2)
        val rootDto = collectedDtos["POST__items_inline"]
        assertNotNull(rootDto)
        val dtoFields = rootDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "numbers", "List<Integer>")
        assertDtoFieldIn(dtoFields, "labels", "List<Labels>")

        val labelsDto = collectedDtos["Labels"]
        assertNotNull(labelsDto)
        val dtoLabelsFields = labelsDto?.fields?:emptyList()
        assertEquals(dtoLabelsFields.size, 1)
        assertDtoFieldIn(dtoLabelsFields, "value", "String")
    }

    @Test
    fun arrayOfComponentsObjectUsesSchemaName() {
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("array/arrayOfComponentsObject.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 3)
        val rootDto = collectedDtos["POST__items_components"]
        assertNotNull(rootDto)
        val dtoFields = rootDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 3)
        assertDtoFieldIn(dtoFields, "numbers", "List<Integer>")
        assertDtoFieldIn(dtoFields, "labels", "List<Label>")
        assertDtoFieldIn(dtoFields, "wrappedStrings", "WrappedString")

        val labelsDto = collectedDtos["Label"]
        assertNotNull(labelsDto)
        val dtoLabelsFields = labelsDto?.fields?:emptyList()
        assertEquals(dtoLabelsFields.size, 1)
        assertDtoFieldIn(dtoLabelsFields, "value", "String")

        val wrappedStringDto = collectedDtos["WrappedString"]
        assertNotNull(wrappedStringDto)
        val dtoWrappedStringFields = wrappedStringDto?.fields?:emptyList()
        assertEquals(dtoWrappedStringFields.size, 1)
        assertDtoFieldIn(dtoWrappedStringFields, "strings", "List<String>")
    }

    @Test
    fun sameDtoInDifferentInlineEndpointsIsDuplicated() {
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("object/duplicateInlineObject.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 2)
        val createDto = collectedDtos["POST__create_user"]
        assertNotNull(createDto)
        val createDtoFields = createDto?.fields?:emptyList()
        assertEquals(createDtoFields.size, 2)
        assertDtoFieldIn(createDtoFields, "name", STRING)
        assertDtoFieldIn(createDtoFields, "age", INTEGER)

        val updateDto = collectedDtos["POST__create_user"]
        assertNotNull(updateDto)
        val updateDtoFields = updateDto?.fields?:emptyList()
        assertEquals(updateDtoFields.size, 2)
        assertDtoFieldIn(updateDtoFields, "name", STRING)
        assertDtoFieldIn(updateDtoFields, "age", INTEGER)
    }

    @Test
    fun whenUsingComponentsDtoIsCollectedOnce() {
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("object/twoEndpointUsingSameComponent.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 1)
        val userDto = collectedDtos["UserDto"]
        assertNotNull(userDto)
        val dtoFields = userDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "name", STRING)
        assertDtoFieldIn(dtoFields, "age", INTEGER)
    }

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
            val dtoFields = oneOfDto?.fields?:emptyList()
            assertEquals(dtoFields.size, 2)
            assertDtoFieldIn(dtoFields, "dog", STRING)
            assertDtoFieldIn(dtoFields, "cat", STRING)
        }
    }


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
        val dtoFields = anyOfDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "email", STRING)
        assertDtoFieldIn(dtoFields, "numbers", "List<Integer>")
    }


    @Test
    fun noDtosWhenNoBodyParam() {
        val dtoWriter = DtoWriter(outputFormat)
        val actionCluster = initRestSchema("noBody.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, MOCK_SOLUTION)

        assertEquals(dtoWriter.getCollectedDtos().size, 0)
    }

    private fun initRestSchema(openApiLocation: String) : Map<String, Action> {
        val restSchema = RestSchema(OpenApiAccess.getOpenAPIFromResource("/swagger/dto-writer/$openApiLocation"))
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)
        return actionCluster
    }

    private fun assertDtoFieldIn(dtoFields: List<DtoField>, targetName: String, targetType: String) {
        assertThat(dtoFields, hasItem(DtoField(targetName, targetType)))
    }
}
