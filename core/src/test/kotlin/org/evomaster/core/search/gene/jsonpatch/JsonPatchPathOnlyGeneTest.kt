package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchPathOnlyGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private fun removeOp(paths: List<String> = listOf("/name", "/email", "/age")) =
        JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", paths))

    // --- Construction ---

    @Test
    fun testGeneNameEqualsOperationNameByDefault() {
        val gene = removeOp()
        assertEquals("remove", gene.name)
        assertEquals("remove", gene.operationName)
    }

    @Test
    fun testCustomGeneNameIsAccepted() {
        val gene = JsonPatchPathOnlyGene("myRemove", "remove", EnumGene("path", listOf("/x")))
        assertEquals("myRemove", gene.name)
    }

    @Test
    fun testHasExactlyOneChild() {
        val gene = removeOp()
        assertEquals(1, gene.getViewOfChildren().size)
        assertSame(gene.pathGene, gene.getViewOfChildren()[0])
    }

    // --- getValueAsPrintableString ---

    @Test
    fun testOutputIsValidJsonObject() {
        val gene = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/age")))
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"remove\",\"path\":\"/age\"}", result)
    }

    @Test
    fun testOutputStartsAndEndsWithBraces() {
        val result = removeOp().getValueAsPrintableString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
    }

    @Test
    fun testOutputContainsOpField() {
        val result = removeOp().getValueAsPrintableString()
        assertTrue(result.contains("\"op\":\"remove\""))
    }

    @Test
    fun testOutputContainsQuotedPath() {
        val gene = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/email")))
        val result = gene.getValueAsPrintableString()
        assertTrue(result.contains("\"path\":\"/email\""))
    }

    // --- copy ---

    @Test
    fun testCopyHasSameValues() {
        val original = removeOp()
        val copy = original.copy() as JsonPatchPathOnlyGene
        assertEquals(original.operationName, copy.operationName)
        assertEquals(original.name, copy.name)
        assertEquals(
            original.getValueAsPrintableString(),
            copy.getValueAsPrintableString()
        )
    }

    @Test
    fun testCopyIsIndependentFromOriginal() {
        val original = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/name", "/age")))
        val copy = original.copy() as JsonPatchPathOnlyGene

        original.pathGene.index = 0
        copy.pathGene.index = 1

        assertNotEquals(
            original.getValueAsPrintableString(),
            copy.getValueAsPrintableString()
        )
    }

    // --- containsSameValueAs ---

    @Test
    fun testContainsSameValueAsTrueForSamePath() {
        val a = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/name")))
        val b = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/name")))
        assertTrue(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsFalseForDifferentPath() {
        val a = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/name", "/age")).apply { index = 0 })
        val b = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/name", "/age")).apply { index = 1 })
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsThrowsForWrongType() {
        assertThrows<IllegalArgumentException> {
            removeOp().containsSameValueAs(JsonPatchFromPathGene("move", "move",
                EnumGene("from", listOf("/")),
                EnumGene("path", listOf("/"))))
        }
    }

    // --- randomize ---

    @Test
    fun testRandomizeCyclesThroughPaths() {
        val gene = JsonPatchPathOnlyGene(
            "remove", "remove",
            EnumGene("path", listOf("/name", "/email", "/age"))
        )
        gene.doInitialize(rand)
        val seenPaths = mutableSetOf<String>()
        repeat(30) {
            gene.randomize(rand, tryToForceNewValue = true)
            seenPaths.add(gene.pathGene.getValueAsRawString())
        }
        assertTrue(seenPaths.size > 1, "Expected multiple paths to be seen, got: $seenPaths")
    }

    @Test
    fun testRandomizeOutputAlwaysStartsWithOpRemove() {
        val gene = removeOp()
        gene.doInitialize(rand)
        repeat(10) {
            gene.randomize(rand, tryToForceNewValue = false)
            assertTrue(gene.getValueAsPrintableString().startsWith("{\"op\":\"remove\""))
        }
    }

    // --- isMutable ---

    @Test
    fun testIsMutableTrueForMultipleValues() {
        assertTrue(removeOp().isMutable())
    }

    @Test
    fun testIsMutableTrueForSingleValue() {
        val gene = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/only")))
        assertTrue(gene.isMutable())
    }

    @Test
    fun testPathEnumWithSingleValueIsNotMutable() {
        val gene = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/only")))
        assertFalse(gene.pathGene.isMutable())
    }
}