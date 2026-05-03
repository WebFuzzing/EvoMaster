package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchFromPathGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private val paths = listOf("/name", "/email", "/age")

    private fun moveOp() = JsonPatchFromPathGene(
        "move", "move",
        fromGene = EnumGene("from", paths),
        pathGene = EnumGene("path", paths)
    )

    private fun copyOp() = JsonPatchFromPathGene(
        "copy", "copy",
        fromGene = EnumGene("from", paths),
        pathGene = EnumGene("path", paths)
    )

    // --- Construction ---

    @Test
    fun testGeneNameEqualsOperationNameByDefault() {
        assertEquals("move", moveOp().name)
        assertEquals("copy", copyOp().name)
    }

    @Test
    fun testCustomGeneNameIsAccepted() {
        val gene = JsonPatchFromPathGene("customMove", "move",
            EnumGene("from", listOf("/")),
            EnumGene("path", listOf("/"))
        )
        assertEquals("customMove", gene.name)
    }

    @Test
    fun testOperationNameIsStoredCorrectly() {
        assertEquals("move", moveOp().operationName)
        assertEquals("copy", copyOp().operationName)
    }

    @Test
    fun testHasExactlyTwoChildren() {
        val gene = moveOp()
        val children = gene.getViewOfChildren()
        assertEquals(2, children.size)
        assertSame(gene.fromGene, children[0])
        assertSame(gene.pathGene, children[1])
    }

    // --- getValueAsPrintableString ---

    @Test
    fun testMoveOutputHasCorrectJsonStructure() {
        val gene = JsonPatchFromPathGene(
            "move", "move",
            fromGene = EnumGene("from", listOf("/name")),
            pathGene = EnumGene("path", listOf("/email"))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"move\",\"from\":\"/name\",\"path\":\"/email\"}", result)
    }

    @Test
    fun testCopyOutputHasCorrectJsonStructure() {
        val gene = JsonPatchFromPathGene(
            "copy", "copy",
            fromGene = EnumGene("from", listOf("/age")),
            pathGene = EnumGene("path", listOf("/name"))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"copy\",\"from\":\"/age\",\"path\":\"/name\"}", result)
    }

    @Test
    fun testOutputIsWrappedInBraces() {
        val result = moveOp().getValueAsPrintableString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
    }

    @Test
    fun testOutputContainsFieldsInOrder() {
        val result = moveOp().getValueAsPrintableString()
        val opIdx   = result.indexOf("\"op\"")
        val fromIdx = result.indexOf("\"from\"")
        val pathIdx = result.indexOf("\"path\"")
        assertTrue(opIdx < fromIdx)
        assertTrue(fromIdx < pathIdx)
    }

    @Test
    fun testFromAndPathAreIndependentlySelectable() {
        val gene = JsonPatchFromPathGene(
            "move", "move",
            fromGene = EnumGene("from", listOf("/a", "/b")).apply { index = 0 },
            pathGene = EnumGene("path", listOf("/a", "/b")).apply { index = 1 }
        )
        val result = gene.getValueAsPrintableString()
        assertTrue(result.contains("\"from\":\"/a\""))
        assertTrue(result.contains("\"path\":\"/b\""))
    }

    // --- copy ---

    @Test
    fun testCopyPreservesValues() {
        val original = moveOp()
        original.fromGene.index = 1
        original.pathGene.index = 2
        val copy = original.copy() as JsonPatchFromPathGene

        assertEquals(original.operationName, copy.operationName)
        assertEquals(original.name, copy.name)
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
    }

    @Test
    fun testCopyIsIndependentFromOriginal() {
        val original = moveOp()
        val copy = original.copy() as JsonPatchFromPathGene

        copy.fromGene.index = 2
        copy.pathGene.index = 0

        assertNotEquals(
            original.getValueAsPrintableString(),
            copy.getValueAsPrintableString()
        )
    }

    // --- containsSameValueAs ---

    @Test
    fun testContainsSameValueAsTrueWhenBothMatch() {
        val a = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", listOf("/y")))
        val b = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", listOf("/y")))
        assertTrue(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsFalseWhenFromDiffers() {
        val pathEnum = listOf("/a", "/b")
        val a = JsonPatchFromPathGene("move", "move", EnumGene("from", pathEnum).apply { index = 0 }, EnumGene("path", listOf("/x")))
        val b = JsonPatchFromPathGene("move", "move", EnumGene("from", pathEnum).apply { index = 1 }, EnumGene("path", listOf("/x")))
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsFalseWhenPathDiffers() {
        val pathEnum = listOf("/a", "/b")
        val a = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", pathEnum).apply { index = 0 })
        val b = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", pathEnum).apply { index = 1 })
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsThrowsForWrongType() {
        assertThrows<IllegalArgumentException> {
            moveOp().containsSameValueAs(JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/"))))
        }
    }

    // --- randomize ---

    @Test
    fun testRandomizeYieldsMultipleCombinations() {
        val gene = moveOp()
        gene.doInitialize(rand)
        val seenValues = mutableSetOf<String>()
        repeat(30) {
            gene.randomize(rand, tryToForceNewValue = true)
            seenValues.add(gene.getValueAsPrintableString())
        }
        assertTrue(seenValues.size > 1, "Expected multiple from-path combinations, got: $seenValues")
    }

    @Test
    fun testRandomizeOutputAlwaysContainsOpField() {
        val gene = moveOp()
        gene.doInitialize(rand)
        repeat(10) {
            gene.randomize(rand, tryToForceNewValue = false)
            val result = gene.getValueAsPrintableString()
            assertTrue(result.contains("\"op\":\"move\""))
        }
    }

    // --- isMutable ---

    @Test
    fun testIsMutableTrueForMultipleValues() {
        assertTrue(moveOp().isMutable())
    }

    @Test
    fun testIsMutableTrueForSingleValues() {
        val gene = JsonPatchFromPathGene("move", "move",
            EnumGene("from", listOf("/only")),
            EnumGene("path", listOf("/only"))
        )
        assertTrue(gene.isMutable())
    }

    @Test
    fun testChildEnumsWithSingleValueNotMutable() {
        val gene = JsonPatchFromPathGene("move", "move",
            EnumGene("from", listOf("/only")),
            EnumGene("path", listOf("/only"))
        )
        assertFalse(gene.fromGene.isMutable())
        assertFalse(gene.pathGene.isMutable())
    }
}