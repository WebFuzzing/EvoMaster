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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DtoWriterTest {

    companion object {
        val tmpTestSuitePath: Path = Paths.get(Files.createTempDirectory("dto-writer-test").toUri())
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
    }

    @Test
    fun javaIsOnlySupportedForDtos() {
        val actionCluster = initRestSchema("/swagger/dto-writer/primitiveTypes.yaml")
        val supportedOutputFormats = listOf(OutputFormat.JAVA_JUNIT_4, OutputFormat.JAVA_JUNIT_5)
        val unsupportedOutputFormats = listOf(OutputFormat.KOTLIN_JUNIT_4, OutputFormat.KOTLIN_JUNIT_5, OutputFormat.JS_JEST,
            OutputFormat.PYTHON_UNITTEST)

        supportedOutputFormats.forEach { outputFormat ->
            val dtoWriter = DtoWriter()
            dtoWriter.write(tmpTestSuitePath, outputFormat, actionCluster.values.map { it.copy() })
            assertTrue(dtoWriter.getCollectedDtos().isNotEmpty())
        }

        unsupportedOutputFormats.forEach { outputFormat ->
            assertThrows(IllegalStateException::class.java, {
                val dtoWriter = DtoWriter()
                dtoWriter.write(tmpTestSuitePath, outputFormat, actionCluster.values.map { it.copy() })
            })
        }
    }

    @Test
    fun emptyActionListReturnsNoDtos() {
        val dtoWriter = DtoWriter()

        dtoWriter.write(tmpTestSuitePath, outputFormat, emptyList())

        assertTrue(dtoWriter.getCollectedDtos().isEmpty())
    }

    @Test
    fun primitiveTypesAreCollectedAsDtoFields() {
        val dtoWriter = DtoWriter()
        val actionCluster = initRestSchema("/swagger/dto-writer/primitiveTypes.yaml")

        dtoWriter.write(tmpTestSuitePath, outputFormat, actionCluster.values.map { it.copy() })

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
        val actionCluster = initRestSchema("/swagger/dto-writer/childObjectInline.yaml")

        dtoWriter.write(tmpTestSuitePath, outputFormat, actionCluster.values.map { it.copy() })

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
        val actionCluster = initRestSchema("/swagger/dto-writer/simpleComponents.yaml")

        dtoWriter.write(tmpTestSuitePath, outputFormat, actionCluster.values.map { it.copy() })

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
        val actionCluster = initRestSchema("/swagger/dto-writer/childObjectComponent.yaml")

        dtoWriter.write(tmpTestSuitePath, outputFormat, actionCluster.values.map { it.copy() })

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

    private fun initRestSchema(openApiLocation: String) : Map<String, Action> {
        val restSchema = RestSchema(OpenApiAccess.getOpenAPIFromResource(openApiLocation))
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)
        return actionCluster
    }

    private fun assertDtoFieldIn(dtoFields: List<DtoField>, targetName: String, targetType: String) {
        assertThat(dtoFields, hasItem(DtoField(targetName, targetType)))
    }
}
