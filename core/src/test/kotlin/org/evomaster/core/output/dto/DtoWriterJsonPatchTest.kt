package org.evomaster.core.output.dto

import org.evomaster.core.TestUtils
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchDocumentGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathValueGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.Collections.singletonList

class DtoWriterJsonPatchTest {

    private val outputTestSuitePath = Paths.get("./target/dto-writer-json-patch-test")
    private val testPackage = "test.package"

    private fun jsonPatchBodyParam(): Param {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val typeGene = EnumGene("contentType", listOf("application/json-patch+json")).apply { index = 0 }
        return BodyParam(gene = JsonPatchDocumentGene("patch", schema), typeGene = typeGene)
    }

    private fun jsonPatchSolution(): Solution<*> {
        val action = getRestCallAction("/items/{id}", HttpVerb.PATCH, mutableListOf(jsonPatchBodyParam()))
        val eIndividual = getEvaluatedIndividualWith(action)
        return Solution(singletonList(eIndividual), "", "", Termination.NONE, emptyList(), emptyList())
    }

    @Test
    fun collectsJsonPatchOperationDtoWithAllFields() {
        val dtoWriter = DtoWriter(OutputFormat.KOTLIN_JUNIT_5)
        dtoWriter.write(outputTestSuitePath, testPackage, jsonPatchSolution())

        val dtos = dtoWriter.getCollectedDtos()
        val operationDto = dtos[GeneToDto.JSON_PATCH_OPERATION_DTO]
        assertNotNull(operationDto, "Expected a ${GeneToDto.JSON_PATCH_OPERATION_DTO} DTO to be collected")

        val fields = operationDto!!.fieldsMap
        assertEquals(DtoField(GeneToDto.FIELD_OP, "String"), fields[GeneToDto.FIELD_OP])
        assertEquals(DtoField(GeneToDto.FIELD_PATH, "String"), fields[GeneToDto.FIELD_PATH])
        assertEquals(DtoField(GeneToDto.FIELD_FROM, "String"), fields[GeneToDto.FIELD_FROM])
        // value is the generic object type since a JSON Patch value can be any JSON value
        assertEquals(DtoField(GeneToDto.FIELD_VALUE, "Any"), fields[GeneToDto.FIELD_VALUE])
    }

    @Test
    fun valueFieldIsObjectForJavaOutput() {
        val dtoWriter = DtoWriter(OutputFormat.JAVA_JUNIT_5)
        dtoWriter.write(outputTestSuitePath, testPackage, jsonPatchSolution())

        val operationDto = dtoWriter.getCollectedDtos()[GeneToDto.JSON_PATCH_OPERATION_DTO]
        assertNotNull(operationDto)
        assertEquals(DtoField(GeneToDto.FIELD_VALUE, "Object"), operationDto!!.fieldsMap[GeneToDto.FIELD_VALUE])
    }

    @Test
    fun collectsNestedObjectDtoWhenValueIsArrayOfObjects() {
        // Schema where the only patchable field is an array of objects:
        // every add/replace/test operation will have an ArrayGene<ObjectGene> as its value gene,
        // so calculateDtoFromJsonPatch must visit calculateDtoFromArray for those operations.
        val tagSchema = ObjectGene("Tag", listOf(StringGene("label")), refType = "Tag")
        val schema = ObjectGene("body", listOf(ArrayGene("tags", tagSchema)))
        val typeGene = EnumGene("contentType", listOf("application/json-patch+json")).apply { index = 0 }
        val bodyParam = BodyParam(gene = JsonPatchDocumentGene("patch", schema), typeGene = typeGene)

        val action = getRestCallAction("/items/{id}", HttpVerb.PATCH, mutableListOf(bodyParam))
        val individual = RestIndividual(mutableListOf(action), SampleType.RANDOM)
        TestUtils.doInitializeIndividualForTesting(individual, Randomness().apply { updateSeed(42L) })

        val result = RestCallResult(action.getLocalId()).apply { setStatusCode(200) }
        val ei = EvaluatedIndividual<RestIndividual>(FitnessValue(0.0), individual, listOf(result))
        val solution = Solution(singletonList(ei), "", "", Termination.NONE, emptyList(), emptyList())

        val dtoWriter = DtoWriter(OutputFormat.KOTLIN_JUNIT_5)
        dtoWriter.write(outputTestSuitePath, testPackage, solution)
        val dtos = dtoWriter.getCollectedDtos()

        assertNotNull(dtos[GeneToDto.JSON_PATCH_OPERATION_DTO])

        val patchGene = bodyParam.primaryGene() as JsonPatchDocumentGene
        val pathValueOps = patchGene.operations.filterIsInstance<JsonPatchPathValueGene>()
        assertTrue(pathValueOps.isNotEmpty(), "Seed 42L must produce at least one add/replace/test operation")
        // All add/replace/test operations carry an ArrayGene<ObjectGene> value in this schema;
        // the nested Tag DTO must therefore be collected.
        assertNotNull(dtos["Tag"], "Nested Tag DTO must be collected for operations with array-of-objects value")
    }
}
