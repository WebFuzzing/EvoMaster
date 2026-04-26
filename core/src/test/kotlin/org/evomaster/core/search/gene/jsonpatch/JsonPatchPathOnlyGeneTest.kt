package org.evomaster.core.search.gene.patch

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchPathOnlyGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private fun removeOp(paths: List<String> = listOf("/name", "/email", "/age")) =
        JsonPatchPathOnlyGene("remove", EnumGene("path", paths))

    // --- Construction ---

    @Test
    fun `default gene name is operationName followed by Op`() {
        val gene = removeOp()
        assertEquals("removeOp", gene.name)
        assertEquals("remove", gene.operationName)
    }

    @Test
    fun `custom gene name is accepted`() {
        val gene = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/x")), geneName = "myRemove")
        assertEquals("myRemove", gene.name)
    }

    @Test
    fun `has exactly one child (pathGene)`() {
        val gene = removeOp()
        assertEquals(1, gene.getViewOfChildren().size)
        assertSame(gene.pathGene, gene.getViewOfChildren()[0])
    }

    // --- getValueAsPrintableString ---

    @Test
    fun `output is a valid JSON object with op and path`() {
        val gene = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/age")))
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"remove\",\"path\":\"/age\"}", result)
    }

    @Test
    fun `output starts and ends with braces`() {
        val result = removeOp().getValueAsPrintableString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
    }

    @Test
    fun `output contains op field with correct value`() {
        val result = removeOp().getValueAsPrintableString()
        assertTrue(result.contains("\"op\":\"remove\""))
    }

    @Test
    fun `output contains path field with quoted JSON pointer`() {
        val gene = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/email")))
        val result = gene.getValueAsPrintableString()
        assertTrue(result.contains("\"path\":\"/email\""))
    }

    // --- copy ---

    @Test
    fun `copy has same operationName and path values`() {
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
    fun `copy is independent from original`() {
        val original = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/name", "/age")))
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
    fun `containsSameValueAs true when same path selected`() {
        val a = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/name")))
        val b = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/name")))
        assertTrue(a.containsSameValueAs(b))
    }

    @Test
    fun `containsSameValueAs false when different path selected`() {
        val a = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/name", "/age")).apply { index = 0 })
        val b = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/name", "/age")).apply { index = 1 })
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun `containsSameValueAs throws for wrong gene type`() {
        assertThrows<IllegalArgumentException> {
            removeOp().containsSameValueAs(JsonPatchFromPathGene("move",
                EnumGene("from", listOf("/")),
                EnumGene("path", listOf("/"))))
        }
    }

    // --- randomize ---

    @Test
    fun `randomize cycles through available paths`() {
        val gene = JsonPatchPathOnlyGene(
            "remove",
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
    fun `randomize output always starts with op remove`() {
        val gene = removeOp()
        gene.doInitialize(rand)
        repeat(10) {
            gene.randomize(rand, tryToForceNewValue = false)
            assertTrue(gene.getValueAsPrintableString().startsWith("{\"op\":\"remove\""))
        }
    }

    // --- isMutable ---

    @Test
    fun `isMutable true when path enum has multiple values`() {
        assertTrue(removeOp().isMutable())
    }

    @Test
    fun `isMutable true even when path enum has single value`() {
        // CompositeFixedGene inherits Gene.isMutable() = true; immutability of children
        // does not propagate upward unless explicitly overridden in the subclass.
        val gene = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/only")))
        assertTrue(gene.isMutable())
    }

    @Test
    fun `path enum with single value is itself not mutable`() {
        val gene = JsonPatchPathOnlyGene("remove", EnumGene("path", listOf("/only")))
        assertFalse(gene.pathGene.isMutable())
    }
}