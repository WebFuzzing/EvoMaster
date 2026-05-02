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
    fun `buildOperationsArray with default paths does not throw`() {
        assertDoesNotThrow { JsonPatchDocumentGeneBuilder.buildOperationsArray() }
    }

    @Test
    fun `buildOperationsArray with empty paths falls back to root`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = emptyList())
        val pathEnum = (array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene)
            .pathGene as EnumGene<*>
        assertEquals(listOf("/"), pathEnum.values)
    }

    @Test
    fun `buildOperationsArray produces template with 6 choices`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertEquals(6, array.template.getViewOfChildren().size)
    }

    @Test
    fun `buildOperationsArray minSize is 1`() {
        assertEquals(1, JsonPatchDocumentGeneBuilder.buildOperationsArray().minSize)
    }

    @Test
    fun `buildOperationsArray maxSize is 10`() {
        assertEquals(10, JsonPatchDocumentGeneBuilder.buildOperationsArray().maxSize)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — operation order and types
    // -------------------------------------------------------------------------

    private fun children(): List<JsonPatchOperationGene> =
        JsonPatchDocumentGeneBuilder.buildOperationsArray().template.getViewOfChildren()
            .map { it as JsonPatchOperationGene }

    @Test
    fun `first choice is remove (JsonPatchPathOnlyGene)`() {
        val op = children()[0]
        assertEquals("remove", op.operationName)
        assertInstanceOf(JsonPatchPathOnlyGene::class.java, op)
    }

    @Test
    fun `second choice is move (JsonPatchFromPathGene)`() {
        val op = children()[1]
        assertEquals("move", op.operationName)
        assertInstanceOf(JsonPatchFromPathGene::class.java, op)
    }

    @Test
    fun `third choice is copy (JsonPatchFromPathGene)`() {
        val op = children()[2]
        assertEquals("copy", op.operationName)
        assertInstanceOf(JsonPatchFromPathGene::class.java, op)
    }

    @Test
    fun `fourth choice is add (JsonPatchPathValueGene)`() {
        val op = children()[3]
        assertEquals("add", op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    @Test
    fun `fifth choice is replace (JsonPatchPathValueGene)`() {
        val op = children()[4]
        assertEquals("replace", op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    @Test
    fun `sixth choice is test (JsonPatchPathValueGene)`() {
        val op = children()[5]
        assertEquals("test", op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — path propagation
    // -------------------------------------------------------------------------

    @Test
    fun `custom paths are present in remove path enum`() {
        val customPaths = listOf("/name", "/age")
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = customPaths)
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue("/name" in paths)
        assertTrue("/age" in paths)
    }

    @Test
    fun `custom paths are present in move from-gene enum`() {
        val customPaths = listOf("/x", "/y")
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(paths = customPaths)
        val moveOp = array.template.getViewOfChildren()[1] as JsonPatchFromPathGene
        val paths = (moveOp.fromGene as EnumGene<*>).values.map { it.toString() }
        assertTrue("/x" in paths)
        assertTrue("/y" in paths)
    }

    @Test
    fun `custom paths are present in add path-value entry enum`() {
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
    fun `path-value operations hold exactly one PairGene entry`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        for (idx in 3..5) {
            val op = array.template.getViewOfChildren()[idx] as JsonPatchPathValueGene
            assertEquals(1, op.pathValueChoice.getViewOfChildren().size)
        }
    }

    @Test
    fun `path-value entry first is EnumGene named path`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entry = addOp.pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        assertInstanceOf(EnumGene::class.java, entry.first)
        assertEquals("path", entry.first.name)
    }

    @Test
    fun `path-value entry second is StringGene named value`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entry = addOp.pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        assertInstanceOf(StringGene::class.java, entry.second)
        assertEquals("value", entry.second.name)
    }

    @Test
    fun `all paths in enums start with slash`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue(paths.all { it.startsWith("/") })
    }

    // -------------------------------------------------------------------------
    // DEFAULT_PATHS
    // -------------------------------------------------------------------------

    @Test
    fun `DEFAULT_PATHS is non-empty`() {
        assertTrue(JsonPatchDocumentGeneBuilder.DEFAULT_PATHS.isNotEmpty())
    }

    @Test
    fun `DEFAULT_PATHS all start with slash`() {
        assertTrue(JsonPatchDocumentGeneBuilder.DEFAULT_PATHS.all { it.startsWith("/") })
    }

    @Test
    fun `buildOperationsArray with ChoiceGene template is a ChoiceGene`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertInstanceOf(ChoiceGene::class.java, array.template)
    }
}