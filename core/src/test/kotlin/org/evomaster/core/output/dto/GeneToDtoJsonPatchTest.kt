package org.evomaster.core.output.dto

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchDocumentGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchFromPathGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathValueGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies that a [JsonPatchDocumentGene] (RFC 6902) is rendered by [GeneToDto] as a
 * List<JsonPatchOperation> instead of stringified JSON.
 */
class GeneToDtoJsonPatchTest {

    private val dtoName = GeneToDto.JSON_PATCH_OPERATION_DTO

    private fun patchDoc(seed: Long = 42L): JsonPatchDocumentGene {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val doc = JsonPatchDocumentGene("patch", schema)
        doc.doInitialize(Randomness().apply { updateSeed(seed) })
        return doc
    }

    private fun setOpValue(line: String): String =
        Regex("""\.setOp\("(.*?)"\)""").find(line)!!.groupValues[1]

    @Test
    fun rendersAsListOfJsonPatchOperationDtosKotlin() {
        val doc = patchDoc()
        val dtoCall = GeneToDto(OutputFormat.KOTLIN_JUNIT_5).getDtoCall(doc, dtoName, mutableListOf(0), false)
        val calls = dtoCall.objectCalls

        assertEquals("list_${dtoName}_0", dtoCall.varName)
        assertEquals("val list_${dtoName}_0 = mutableListOf<$dtoName>()", calls.first())

        val ops = doc.operations
        // One object instantiation and one add-to-list per operation
        assertEquals(ops.size, calls.count { it.contains("= $dtoName()") })
        assertEquals(ops.size, calls.count { it.startsWith("list_${dtoName}_0.add(dto_${dtoName}_") })

        // Every operation sets its "op", and the multiset of op names matches the document
        val opNamesInCode = calls.filter { it.contains(".setOp(") }.map { setOpValue(it) }
        assertEquals(ops.map { it.operationName }.sorted(), opNamesInCode.sorted())
    }

    @Test
    fun setsOnlyTheFieldsRelevantToEachOperation() {
        val doc = patchDoc()
        val calls = GeneToDto(OutputFormat.KOTLIN_JUNIT_5).getDtoCall(doc, dtoName, mutableListOf(0), false).objectCalls

        val ops = doc.operations
        // "from" only for move/copy, "value" only for add/replace/test
        assertEquals(ops.count { it is JsonPatchFromPathGene }, calls.count { it.contains(".setFrom(") })
        assertEquals(ops.count { it is JsonPatchPathValueGene }, calls.count { it.contains(".setValue(") })
        // "op" and "path" are present in every operation
        assertEquals(ops.size, calls.count { it.contains(".setOp(") })
        assertEquals(ops.size, calls.count { it.contains(".setPath(") })
    }

    @Test
    fun primitiveValuesAreInlinedAsLiterals() {
        // Force every operation type to render; assert string values are quoted and int values are bare
        val doc = patchDoc(seed = 7L)
        val calls = GeneToDto(OutputFormat.KOTLIN_JUNIT_5).getDtoCall(doc, dtoName, mutableListOf(0), false).objectCalls

        calls.filter { it.contains(".setValue(") }.forEach { line ->
            val value = Regex("""\.setValue\((.*)\)""").find(line)!!.groupValues[1]
            val isQuotedString = value.startsWith("\"") && value.endsWith("\"")
            val isInt = value.toIntOrNull() != null
            assertTrue(isQuotedString || isInt, "Unexpected non-literal value rendering: $line")
        }
    }

    @Test
    fun rendersWithJavaSyntax() {
        val doc = patchDoc()
        val calls = GeneToDto(OutputFormat.JAVA_JUNIT_5).getDtoCall(doc, dtoName, mutableListOf(0), false).objectCalls

        assertEquals("List<$dtoName> list_${dtoName}_0 = new ArrayList<$dtoName>();", calls.first())
        assertTrue(calls.any { it.contains("$dtoName dto_${dtoName}_0_1 = new $dtoName();") })
    }
}
