package org.evomaster.core.output.dto

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchDocumentGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.Collections.singletonList

/**
 * Verifies that [DtoWriter] collects the shared JsonPatchOperation DTO (RFC 6902) with every
 * field used across operation types, so a JSON Patch payload can be written as a DTO.
 */
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
}
