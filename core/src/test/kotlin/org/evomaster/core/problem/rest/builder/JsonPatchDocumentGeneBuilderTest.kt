package org.evomaster.core.problem.rest.builder

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchFromPathGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchOperationGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathOnlyGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathValueGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchDocumentGeneBuilderTest {

    // -------------------------------------------------------------------------
    // buildOperationsArray — structure
    // -------------------------------------------------------------------------

    @Test
    fun testBuildOperationsArrayWithDefaultPaths() {
        assertDoesNotThrow { JsonPatchDocumentGeneBuilder.buildOperationsArray() }
    }

    @Test
    fun testBuildOperationsArrayEmptyPathsFallsBackToRoot() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = emptyList())
        val pathEnum = (array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene)
            .pathGene as EnumGene<*>
        assertEquals(listOf("/"), pathEnum.values)
    }

    @Test
    fun testBuildOperationsArrayProduces6Choices() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertEquals(6, array.template.getViewOfChildren().size)
    }

    @Test
    fun testBuildOperationsArrayMinSizeIs1() {
        assertEquals(1, JsonPatchDocumentGeneBuilder.buildOperationsArray().minSize)
    }

    @Test
    fun testBuildOperationsArrayMaxSizeIs10() {
        assertEquals(10, JsonPatchDocumentGeneBuilder.buildOperationsArray().maxSize)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — operation order and types
    // -------------------------------------------------------------------------

    private fun children(): List<JsonPatchOperationGene> =
        JsonPatchDocumentGeneBuilder.buildOperationsArray().template.getViewOfChildren()
            .map { it as JsonPatchOperationGene }

    @Test
    fun testFirstChoiceIsRemove() {
        val op = children()[0]
        assertEquals("remove", op.operationName)
        assertInstanceOf(JsonPatchPathOnlyGene::class.java, op)
    }

    @Test
    fun testSecondChoiceIsMove() {
        val op = children()[1]
        assertEquals("move", op.operationName)
        assertInstanceOf(JsonPatchFromPathGene::class.java, op)
    }

    @Test
    fun testThirdChoiceIsCopy() {
        val op = children()[2]
        assertEquals("copy", op.operationName)
        assertInstanceOf(JsonPatchFromPathGene::class.java, op)
    }

    @Test
    fun testFourthChoiceIsAdd() {
        val op = children()[3]
        assertEquals("add", op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    @Test
    fun testFifthChoiceIsReplace() {
        val op = children()[4]
        assertEquals("replace", op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    @Test
    fun testSixthChoiceIsTest() {
        val op = children()[5]
        assertEquals("test", op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — path propagation
    // -------------------------------------------------------------------------

    @Test
    fun testCustomPathsInRemovePathEnum() {
        val customPaths = listOf("/name", "/age")
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = customPaths)
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue("/name" in paths)
        assertTrue("/age" in paths)
    }

    @Test
    fun testCustomPathsInMoveFromGeneEnum() {
        val customPaths = listOf("/x", "/y")
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = customPaths)
        val moveOp = array.template.getViewOfChildren()[1] as JsonPatchFromPathGene
        val paths = (moveOp.fromGene as EnumGene<*>).values.map { it.toString() }
        assertTrue("/x" in paths)
        assertTrue("/y" in paths)
    }

    @Test
    fun testCustomPathsInAddPathValueEntryEnum() {
        val customPaths = listOf("/foo", "/bar")
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = customPaths)
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entry = addOp.pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        val paths = (entry.first as EnumGene<*>).values.map { it.toString() }
        assertTrue("/foo" in paths)
        assertTrue("/bar" in paths)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — path-value entries
    // -------------------------------------------------------------------------

    @Test
    fun testPathValueOperationsHoldOnePairGeneEntry() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        for (idx in 3..5) {
            val op = array.template.getViewOfChildren()[idx] as JsonPatchPathValueGene
            assertEquals(1, op.pathValueChoice.getViewOfChildren().size)
        }
    }

    @Test
    fun testPathValueEntryFirstIsEnumGeneNamedPath() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entry = addOp.pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        assertInstanceOf(EnumGene::class.java, entry.first)
        assertEquals("path", entry.first.name)
    }

    @Test
    fun testPathValueEntrySecondIsStringGeneNamedValue() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entry = addOp.pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        assertInstanceOf(StringGene::class.java, entry.second)
        assertEquals("value", entry.second.name)
    }

    @Test
    fun testAllPathsInEnumsStartWithSlash() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue(paths.all { it.startsWith("/") })
    }

    // -------------------------------------------------------------------------
    // DEFAULT_PATHS
    // -------------------------------------------------------------------------

    @Test
    fun testDefaultPathsIsNonEmpty() {
        assertTrue(JsonPatchDocumentGeneBuilder.DEFAULT_PATHS.isNotEmpty())
    }

    @Test
    fun testDefaultPathsAllStartWithSlash() {
        assertTrue(JsonPatchDocumentGeneBuilder.DEFAULT_PATHS.all { it.startsWith("/") })
    }

    @Test
    fun testBuildOperationsArrayTemplateIsChoiceGene() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertInstanceOf(ChoiceGene::class.java, array.template)
    }
}