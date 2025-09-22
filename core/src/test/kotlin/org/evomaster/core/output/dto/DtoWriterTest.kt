package org.evomaster.core.output.dto

import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.search.action.Action
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

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
    }

    @Test
    fun javaIsOnlySupportedForDtos() {
        val actionCluster = initRestSchema("primitiveTypes.yaml")
        val supportedOutputFormats = listOf(OutputFormat.JAVA_JUNIT_4, OutputFormat.JAVA_JUNIT_5)
        val unsupportedOutputFormats = listOf(OutputFormat.KOTLIN_JUNIT_4, OutputFormat.KOTLIN_JUNIT_5, OutputFormat.JS_JEST,
            OutputFormat.PYTHON_UNITTEST)

        supportedOutputFormats.forEach { outputFormat ->
            val dtoWriter = DtoWriter()
            dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })
            assertTrue(dtoWriter.getCollectedDtos().isNotEmpty())
        }

        unsupportedOutputFormats.forEach { outputFormat ->
            assertThrows(IllegalStateException::class.java, {
                val dtoWriter = DtoWriter()
                dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })
            })
        }
    }

    @Test
    fun emptyActionListReturnsNoDtos() {
        val dtoWriter = DtoWriter()

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, emptyList())

        assertTrue(dtoWriter.getCollectedDtos().isEmpty())
    }

    @Test
    fun primitiveTypesAreCollectedAsDtoFields() {
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("primitiveTypes.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 1)
        val postExampleDto = collectedDtos["POST__example"]
        assertNotNull(postExampleDto)
        val dtoFields = postExampleDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 12)
        assertDtoFieldIn(dtoFields, "aString", STRING)
        assertDtoFieldIn(dtoFields, "aRegex", STRING)
        assertDtoFieldIn(dtoFields, "aBase64String", STRING)
        assertDtoFieldIn(dtoFields, "aDate", STRING)
        assertDtoFieldIn(dtoFields, "aTime", STRING)
        assertDtoFieldIn(dtoFields, "aDateTime", STRING)
        assertDtoFieldIn(dtoFields, "anInteger", INTEGER)
        assertDtoFieldIn(dtoFields, "aLong", "Long")
        assertDtoFieldIn(dtoFields, "aNumber", "Double")
        assertDtoFieldIn(dtoFields, "aFloat", "Float")
        assertDtoFieldIn(dtoFields, "aBoolean", BOOLEAN)
        assertDtoFieldIn(dtoFields, "aNullableString", STRING)
    }

    @Test
    fun childObjectInlineIsCollectedInDto() {
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("object/childObjectInline.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 2)
        val postExampleDto = collectedDtos["POST__example"]
        assertNotNull(postExampleDto)
        val dtoFields = postExampleDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "aString", STRING)
        assertDtoFieldIn(dtoFields, "anObject", "Anobject")

        val childObjectDto = collectedDtos["Anobject"]
        assertNotNull(childObjectDto)
        val childObjectDtoFields = childObjectDto?.fields?:emptyList()
        assertEquals(childObjectDtoFields.size, 2)
        assertDtoFieldIn(childObjectDtoFields, "name", STRING)
        assertDtoFieldIn(childObjectDtoFields, "age", INTEGER)
    }

    @Test
    fun whenUsingComponentsDtoNameIsSchemaName() {
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("object/simpleComponents.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 2)
        val firstSchema = collectedDtos["FirstSchema"]
        assertNotNull(firstSchema)
        val dtoFields = firstSchema?.fields?:emptyList()
        assertEquals(dtoFields.size, 1)
        assertDtoFieldIn(dtoFields, "message", STRING)

        val secondSchema = collectedDtos["SecondSchema"]
        assertNotNull(secondSchema)
        val childObjectDtoFields = secondSchema?.fields?:emptyList()
        assertEquals(childObjectDtoFields.size, 1)
        assertDtoFieldIn(childObjectDtoFields, "active", BOOLEAN)
    }

    @Test
    fun childObjectComponentIsCollectedInDto() {
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("object/childObjectComponent.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

        val collectedDtos = dtoWriter.getCollectedDtos()
        assertEquals(collectedDtos.size, 2)
        val postExampleDto = collectedDtos["ParentSchema"]
        assertNotNull(postExampleDto)
        val dtoFields = postExampleDto?.fields?:emptyList()
        assertEquals(dtoFields.size, 2)
        assertDtoFieldIn(dtoFields, "label", STRING)
        assertDtoFieldIn(dtoFields, "child", "ChildSchema")

        val childObjectDto = collectedDtos["ChildSchema"]
        assertNotNull(childObjectDto)
        val childObjectDtoFields = childObjectDto?.fields?:emptyList()
        assertEquals(childObjectDtoFields.size, 2)
        assertDtoFieldIn(childObjectDtoFields, "name", STRING)
        assertDtoFieldIn(childObjectDtoFields, "age", INTEGER)
    }

    @Test
    fun arrayAsRootTypeCollectsASingleDto() {
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("array/rootArrayWithComponents.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

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
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("array/arrayOfInlineObject.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

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
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("array/arrayOfComponentsObject.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

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
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("object/duplicateInlineObject.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

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
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("object/twoEndpointUsingSameComponent.yaml")

        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })

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
            val dtoWriter = DtoWriter()
            val actionCluster = initRestSchema("choice/oneOf$chosenDto.yaml")
            dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })
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
    fun testAnyOfMergesDtosIntoASingleOne() {
        val dtoSpecs = listOf("Components", "Inline", "MixedOptional")
        dtoSpecs.forEach { chosenDto ->
            val dtoWriter = DtoWriter()
            val actionCluster = initRestSchema("choice/anyOf$chosenDto.yaml")
            dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })
            val collectedDtos = dtoWriter.getCollectedDtos()
            assertEquals(collectedDtos.size, 1)
            val anyOfDto = collectedDtos[collectedDtos.keys.first()]
            assertNotNull(anyOfDto)
            val dtoFields = anyOfDto?.fields?:emptyList()
            assertEquals(dtoFields.size, 2)
            assertDtoFieldIn(dtoFields, "phone", STRING)
            assertDtoFieldIn(dtoFields, "email", STRING)
        }
    }

    @Test
    fun testAnyOfArrayAndObject() {
        val chosenDto = "ArrayAndObject"
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("choice/anyOf$chosenDto.yaml")
        dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })
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
    fun testAllOfMergesDtosIntoASingleOne() {
        val dtoSpecs = listOf("Components", "Inline", "Mixed")
        dtoSpecs.forEach { chosenDto ->
            val dtoWriter = DtoWriter()
            val actionCluster = initRestSchema("choice/allOf$chosenDto.yaml")
            dtoWriter.write(outputTestSuitePath, TEST_PACKAGE, outputFormat, actionCluster.values.map { it.copy() })
            val collectedDtos = dtoWriter.getCollectedDtos()
            assertEquals(collectedDtos.size, 1)
            val allOfDto = collectedDtos[collectedDtos.keys.first()]
            assertNotNull(allOfDto)
            val dtoFields = allOfDto?.fields?:emptyList()
            assertEquals(dtoFields.size, 2)
            assertDtoFieldIn(dtoFields, "name", STRING)
            assertDtoFieldIn(dtoFields, "age", INTEGER)
        }
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
