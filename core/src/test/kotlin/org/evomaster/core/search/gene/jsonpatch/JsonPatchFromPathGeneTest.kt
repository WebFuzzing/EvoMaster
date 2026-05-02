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
    fun `gene name equals operationName by default`() {
        assertEquals("move", moveOp().name)
        assertEquals("copy", copyOp().name)
    }

    @Test
    fun `custom gene name is accepted`() {
        val gene = JsonPatchFromPathGene("customMove", "move",
            EnumGene("from", listOf("/")),
            EnumGene("path", listOf("/"))
        )
        assertEquals("customMove", gene.name)
    }

    @Test
    fun `operationName is stored correctly`() {
        assertEquals("move", moveOp().operationName)
        assertEquals("copy", copyOp().operationName)
    }

    @Test
    fun `has exactly two children (fromGene then pathGene)`() {
        val gene = moveOp()
        val children = gene.getViewOfChildren()
        assertEquals(2, children.size)
        assertSame(gene.fromGene, children[0])
        assertSame(gene.pathGene, children[1])
    }

    // --- getValueAsPrintableString ---

    @Test
    fun `move output has correct JSON structure`() {
        val gene = JsonPatchFromPathGene(
            "move", "move",
            fromGene = EnumGene("from", listOf("/name")),
            pathGene = EnumGene("path", listOf("/email"))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"move\",\"from\":\"/name\",\"path\":\"/email\"}", result)
    }

    @Test
    fun `copy output has correct JSON structure`() {
        val gene = JsonPatchFromPathGene(
            "copy", "copy",
            fromGene = EnumGene("from", listOf("/age")),
            pathGene = EnumGene("path", listOf("/name"))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"copy\",\"from\":\"/age\",\"path\":\"/name\"}", result)
    }

    @Test
    fun `output is wrapped in braces`() {
        val result = moveOp().getValueAsPrintableString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
    }

    @Test
    fun `output contains op, from, and path fields in order`() {
        val result = moveOp().getValueAsPrintableString()
        val opIdx   = result.indexOf("\"op\"")
        val fromIdx = result.indexOf("\"from\"")
        val pathIdx = result.indexOf("\"path\"")
        assertTrue(opIdx < fromIdx)
        assertTrue(fromIdx < pathIdx)
    }

    @Test
    fun `from and path are independently selectable`() {
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
    fun `copy preserves operationName and current values`() {
        val original = moveOp()
        original.fromGene.index = 1
        original.pathGene.index = 2
        val copy = original.copy() as JsonPatchFromPathGene

        assertEquals(original.operationName, copy.operationName)
        assertEquals(original.name, copy.name)
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
    }

    @Test
    fun `copy is independent — changing copy does not affect original`() {
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
    fun `containsSameValueAs true when from and path both match`() {
        val a = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", listOf("/y")))
        val b = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", listOf("/y")))
        assertTrue(a.containsSameValueAs(b))
    }

    @Test
    fun `containsSameValueAs false when from differs`() {
        val pathEnum = listOf("/a", "/b")
        val a = JsonPatchFromPathGene("move", "move", EnumGene("from", pathEnum).apply { index = 0 }, EnumGene("path", listOf("/x")))
        val b = JsonPatchFromPathGene("move", "move", EnumGene("from", pathEnum).apply { index = 1 }, EnumGene("path", listOf("/x")))
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun `containsSameValueAs false when path differs`() {
        val pathEnum = listOf("/a", "/b")
        val a = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", pathEnum).apply { index = 0 })
        val b = JsonPatchFromPathGene("move", "move", EnumGene("from", listOf("/x")), EnumGene("path", pathEnum).apply { index = 1 })
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun `containsSameValueAs throws for wrong gene type`() {
        assertThrows<IllegalArgumentException> {
            moveOp().containsSameValueAs(JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/"))))
        }
    }

    // --- randomize ---

    @Test
    fun `randomize yields multiple from-path combinations`() {
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
    fun `randomize output always contains op field`() {
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
    fun `isMutable true when at least one enum has multiple values`() {
        assertTrue(moveOp().isMutable())
    }

    @Test
    fun `isMutable true even when both enums have only one value`() {
        val gene = JsonPatchFromPathGene("move", "move",
            EnumGene("from", listOf("/only")),
            EnumGene("path", listOf("/only"))
        )
        assertTrue(gene.isMutable())
    }

    @Test
    fun `child enums with single value are themselves not mutable`() {
        val gene = JsonPatchFromPathGene("move", "move",
            EnumGene("from", listOf("/only")),
            EnumGene("path", listOf("/only"))
        )
        assertFalse(gene.fromGene.isMutable())
        assertFalse(gene.pathGene.isMutable())
    }
}