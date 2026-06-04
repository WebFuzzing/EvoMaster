package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchFromPathGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private val paths = listOf("/name", "/email", "/age")

    private fun moveOp() = JsonPatchFromPathGene(
        JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
        fromGene = EnumGene("from", paths),
        pathGene = EnumGene("path", paths)
    )

    private fun copyOp() = JsonPatchFromPathGene(
        JsonPatchOperationGene.OP_COPY, JsonPatchOperationGene.OP_COPY,
        fromGene = EnumGene("from", paths),
        pathGene = EnumGene("path", paths)
    )

    // --- Construction ---

    @Test
    fun testGeneNameEqualsOperationNameByDefault() {
        assertEquals(JsonPatchOperationGene.OP_MOVE, moveOp().name)
        assertEquals(JsonPatchOperationGene.OP_COPY, copyOp().name)
    }

    @Test
    fun testCustomGeneNameIsAccepted() {
        val gene = JsonPatchFromPathGene("customMove", JsonPatchOperationGene.OP_MOVE,
            EnumGene("from", listOf("/")),
            EnumGene("path", listOf("/"))
        )
        assertEquals("customMove", gene.name)
    }

    @Test
    fun testOperationNameIsStoredCorrectly() {
        assertEquals(JsonPatchOperationGene.OP_MOVE, moveOp().operationName)
        assertEquals(JsonPatchOperationGene.OP_COPY, copyOp().operationName)
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
            JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            fromGene = EnumGene("from", listOf("/name")),
            pathGene = EnumGene("path", listOf("/email"))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"move\",\"from\":\"/name\",\"path\":\"/email\"}", result)
    }

    @Test
    fun testCopyOutputHasCorrectJsonStructure() {
        val gene = JsonPatchFromPathGene(
            JsonPatchOperationGene.OP_COPY, JsonPatchOperationGene.OP_COPY,
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
            JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            fromGene = EnumGene("from", listOf("/a", "/b")).apply { index = 0 },
            pathGene = EnumGene("path", listOf("/a", "/b")).apply { index = 1 }
        )
        val result = gene.getValueAsPrintableString()
        assertTrue(result.contains("\"from\":\"/a\""))
        assertTrue(result.contains("\"path\":\"/b\""))
    }

    // --- XML serialization ---

    @Test
    fun testMoveOutputInXmlMode() {
        val gene = JsonPatchFromPathGene(
            JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            fromGene = EnumGene("from", listOf("/a")),
            pathGene  = EnumGene("path", listOf("/b"))
        )
        val result = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<operation><op>move</op><from>/a</from><path>/b</path></operation>", result)
    }

    @Test
    fun testCopyOutputInXmlMode() {
        val gene = JsonPatchFromPathGene(
            JsonPatchOperationGene.OP_COPY, JsonPatchOperationGene.OP_COPY,
            fromGene = EnumGene("from", listOf("/x")),
            pathGene  = EnumGene("path", listOf("/y"))
        )
        val result = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<operation><op>copy</op><from>/x</from><path>/y</path></operation>", result)
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
        val a = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE, EnumGene("from", listOf("/x")), EnumGene("path", listOf("/y")))
        val b = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE, EnumGene("from", listOf("/x")), EnumGene("path", listOf("/y")))
        assertTrue(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsFalseWhenFromDiffers() {
        val pathEnum = listOf("/a", "/b")
        val a = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE, EnumGene("from", pathEnum).apply { index = 0 }, EnumGene("path", listOf("/x")))
        val b = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE, EnumGene("from", pathEnum).apply { index = 1 }, EnumGene("path", listOf("/x")))
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsFalseWhenPathDiffers() {
        val pathEnum = listOf("/a", "/b")
        val a = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE, EnumGene("from", listOf("/x")), EnumGene("path", pathEnum).apply { index = 0 })
        val b = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE, EnumGene("from", listOf("/x")), EnumGene("path", pathEnum).apply { index = 1 })
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsThrowsForWrongType() {
        assertThrows<IllegalArgumentException> {
            moveOp().containsSameValueAs(JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, EnumGene("path", listOf("/"))))
        }
    }

    // --- unsafeCopyValueFrom ---

    @Test
    fun testUnsafeCopyValueFromSameTypeCopiesValues() {
        val source = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            fromGene = EnumGene("from", paths).apply { index = 2 },
            pathGene = EnumGene("path", paths).apply { index = 1 })
        val target = moveOp()
        assertTrue(target.unsafeCopyValueFrom(source))
        assertEquals(source.getValueAsPrintableString(), target.getValueAsPrintableString())
    }

    @Test
    fun testUnsafeCopyValueFromWrongTypeReturnsFalse() {
        assertFalse(moveOp().unsafeCopyValueFrom(
            JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, EnumGene("path", listOf("/")))
        ))
    }

    @Test
    fun testUnsafeCopyValueFromDifferentOperationReturnsFalse() {
        assertFalse(moveOp().unsafeCopyValueFrom(copyOp()))
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
        val gene = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            EnumGene("from", listOf("/only")),
            EnumGene("path", listOf("/only"))
        )
        assertTrue(gene.isMutable())
    }

    @Test
    fun testChildEnumsWithSingleValueNotMutable() {
        val gene = JsonPatchFromPathGene(JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            EnumGene("from", listOf("/only")),
            EnumGene("path", listOf("/only"))
        )
        assertFalse(gene.fromGene.isMutable())
        assertFalse(gene.pathGene.isMutable())
    }
}