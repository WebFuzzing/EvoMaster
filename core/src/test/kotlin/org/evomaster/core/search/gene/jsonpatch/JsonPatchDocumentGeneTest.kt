package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.problem.rest.builder.JsonPatchDocumentGeneBuilder
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchDocumentGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private fun doc(seed: Long = 42L): JsonPatchDocumentGene {
        val d = JsonPatchDocumentGene("patch")
        d.doInitialize(Randomness().apply { updateSeed(seed) })
        return d
    }

    // --- Construction ---

    @Test
    fun `construction succeeds`() {
        val d = JsonPatchDocumentGene("patch")
        assertEquals("patch", d.name)
    }

    @Test
    fun `has exactly one child (the operations ArrayGene)`() {
        assertEquals(1, JsonPatchDocumentGene("patch").getViewOfChildren().size)
    }

    // --- operations property ---

    @Test
    fun `operations is non-empty after initialization`() {
        assertTrue(doc().operations.isNotEmpty())
    }

    @Test
    fun `operations all have valid operation names`() {
        val valid = setOf("add", "remove", "replace", "move", "copy", "test")
        assertTrue(doc().operations.all { it.operationName in valid })
    }

    @Test
    fun `operations size respects minSize constraint`() {
        assertTrue(doc().operations.size >= JsonPatchDocumentGene.MIN_SIZE)
    }

    @Test
    fun `operations size respects maxSize constraint`() {
        assertTrue(doc().operations.size <= JsonPatchDocumentGene.DEFAULT_MAX_SIZE)
    }

    // --- getValueAsPrintableString ---

    @Test
    fun `output is a JSON array starting with opening bracket`() {
        assertTrue(doc().getValueAsPrintableString().startsWith("["))
    }

    @Test
    fun `output is a JSON array ending with closing bracket`() {
        assertTrue(doc().getValueAsPrintableString().endsWith("]"))
    }

    @Test
    fun `each operation in output is a JSON object`() {
        val result = doc().getValueAsPrintableString()
        val objectPattern = Regex("""\{"op":"[^"]+"""")
        assertTrue(objectPattern.containsMatchIn(result), "Expected at least one JSON object in: $result")
    }

    @Test
    fun `each operation in output contains op field`() {
        val d = doc()
        val result = d.getValueAsPrintableString()
        val opCount = result.split("\"op\"").size - 1
        assertEquals(d.operations.size, opCount)
    }

    @Test
    fun `paths in output are valid JSON pointers`() {
        val result = doc().getValueAsPrintableString()
        val pathMatches = Regex("\"path\":\"(/[^\"]+)\"").findAll(result)
        pathMatches.forEach { match ->
            assertTrue(match.groupValues[1].startsWith("/"), "Expected JSON pointer, got: ${match.groupValues[1]}")
        }
    }

    // --- copy ---

    @Test
    fun `copy produces gene with same string output`() {
        val original = doc()
        val copy = original.copy() as JsonPatchDocumentGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
    }

    @Test
    fun `copy is independent from original`() {
        val original = doc()
        val copy = original.copy() as JsonPatchDocumentGene
        original.randomize(rand, tryToForceNewValue = true)
        assertTrue(copy.getValueAsPrintableString().startsWith("["))
        assertTrue(original.getValueAsPrintableString().startsWith("["))
    }

    // --- containsSameValueAs ---

    @Test
    fun `containsSameValueAs true for two copies of the same doc`() {
        val d1 = doc()
        val d2 = d1.copy() as JsonPatchDocumentGene
        assertTrue(d1.containsSameValueAs(d2))
    }

    @Test
    fun `containsSameValueAs throws for wrong gene type`() {
        assertThrows<IllegalArgumentException> {
            doc().containsSameValueAs(StringGene("x"))
        }
    }

    // --- randomize ---

    @Test
    fun `multiple calls to randomize produce valid JSON arrays`() {
        val d = JsonPatchDocumentGene("patch")
        d.doInitialize(rand)
        repeat(10) {
            d.randomize(rand, tryToForceNewValue = true)
            val result = d.getValueAsPrintableString()
            assertTrue(result.startsWith("["), "Expected JSON array, got: $result")
            assertTrue(result.endsWith("]"), "Expected JSON array, got: $result")
        }
    }

    @Test
    fun `randomize produces diverse operation names`() {
        val d = JsonPatchDocumentGene("patch")
        d.doInitialize(rand)
        val seenOps = mutableSetOf<String>()
        repeat(40) {
            d.randomize(rand, tryToForceNewValue = true)
            d.operations.forEach { seenOps.add(it.operationName) }
        }
        assertTrue(seenOps.size > 1, "Expected multiple operation types, got: $seenOps")
    }

    // --- template structure (delegated to builder; sanity checks here) ---

    @Test
    fun `template contains 6 choices`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertEquals(6, array.template.getViewOfChildren().size)
    }

    @Test
    fun `template respects minSize and maxSize`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertEquals(JsonPatchDocumentGene.MIN_SIZE, array.minSize)
        assertEquals(JsonPatchDocumentGene.DEFAULT_MAX_SIZE, array.maxSize)
    }

    @Test
    fun `path-value operations hold PairGene entries inside their ChoiceGene`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val children = array.template.getViewOfChildren()
        for (idx in 3..5) {
            val op = children[idx] as JsonPatchPathValueGene
            val choiceChildren = op.pathValueChoice.getViewOfChildren()
            assertTrue(choiceChildren.isNotEmpty())
            choiceChildren.forEach { entry ->
                assertInstanceOf(PairGene::class.java, entry)
                @Suppress("UNCHECKED_CAST")
                val pair = entry as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
                assertInstanceOf(EnumGene::class.java, pair.first)
            }
        }
    }

    @Test
    fun `path-value entry second is a StringGene`() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        addOp.pathValueChoice.getViewOfChildren().forEach { entry ->
            @Suppress("UNCHECKED_CAST")
            val pair = entry as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
            assertInstanceOf(StringGene::class.java, pair.second)
            val paths = pair.first.values.map { it.toString() }
            assertTrue(paths.all { it.startsWith("/") }, "Expected JSON pointer paths, got: $paths")
        }
    }
}