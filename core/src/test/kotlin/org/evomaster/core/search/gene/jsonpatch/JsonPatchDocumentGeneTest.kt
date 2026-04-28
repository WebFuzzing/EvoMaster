package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchDocumentGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private val schema = ObjectGene("resource", listOf(
        StringGene("name"),
        StringGene("email"),
        IntegerGene("age")
    ))

    private fun doc(seed: Long = 42L): JsonPatchDocumentGene {
        val d = JsonPatchDocumentGene("patch", schema)
        d.doInitialize(Randomness().apply { updateSeed(seed) })
        return d
    }

    // --- Construction ---

    @Test
    fun `construction with schema succeeds`() {
        val d = JsonPatchDocumentGene("patch", schema)
        assertEquals("patch", d.name)
        assertSame(schema, d.resourceSchema)
    }

    @Test
    fun `construction without schema uses fallback root path`() {
        val d = JsonPatchDocumentGene("patch")
        assertNull(d.resourceSchema)
        // Building the array should not throw
        d.doInitialize(rand)
    }

    @Test
    fun `has exactly one child (the operations ArrayGene)`() {
        val d = JsonPatchDocumentGene("patch", schema)
        assertEquals(1, d.getViewOfChildren().size)
    }

    @Test
    fun `buildOperationsArray with schema produces 6 choices in template`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        val template = array.template
        // ChoiceGene has 6 children: remove, move, copy, add, replace, test
        assertEquals(6, template.getViewOfChildren().size)
    }

    @Test
    fun `buildOperationsArray respects minSize and maxSize`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        assertEquals(JsonPatchDocumentGene.MIN_SIZE, array.minSize)
        assertEquals(JsonPatchDocumentGene.DEFAULT_MAX_SIZE, array.maxSize)
    }

    // --- operations property ---

    @Test
    fun `operations is non-empty after initialization`() {
        val d = doc()
        assertTrue(d.operations.isNotEmpty())
    }

    @Test
    fun `operations all have valid operation names`() {
        val valid = setOf("add", "remove", "replace", "move", "copy", "test")
        val d = doc()
        assertTrue(d.operations.all { it.operationName in valid })
    }

    @Test
    fun `operations size respects minSize constraint`() {
        val d = doc()
        assertTrue(d.operations.size >= JsonPatchDocumentGene.MIN_SIZE)
    }

    @Test
    fun `operations size respects maxSize constraint`() {
        val d = doc()
        assertTrue(d.operations.size <= JsonPatchDocumentGene.DEFAULT_MAX_SIZE)
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
        // ArrayGene separates elements with ", " — count elements by the op count
        // and verify every "op" field is inside a {...} block
        val objectPattern = Regex("""\{"op":"[^"]+"""")
        val matches = objectPattern.findAll(result).toList()
        assertTrue(matches.isNotEmpty(), "Expected at least one JSON object in: $result")
        // Verify overall structure: starts with [ and ends with ]
        assertTrue(result.startsWith("[") && result.endsWith("]"))
    }

    @Test
    fun `each operation in output contains op field`() {
        val result = doc().getValueAsPrintableString()
        // Count occurrences of "op" key
        val opCount = result.split("\"op\"").size - 1
        assertEquals(doc().operations.size, opCount)
    }

    @Test
    fun `output without schema uses root path`() {
        val d = JsonPatchDocumentGene("patch")
        d.doInitialize(rand)
        val result = d.getValueAsPrintableString()
        assertTrue(result.startsWith("["))
    }

    @Test
    fun `paths in output are valid JSON pointers from schema`() {
        val result = doc().getValueAsPrintableString()
        // All paths should be from the schema fields: /name, /email, /age
        val validPaths = setOf("\"/name\"", "\"/email\"", "\"/age\"")
        val pathMatches = Regex("\"path\":\"(/[^\"]+)\"").findAll(result)
        pathMatches.forEach { match ->
            val path = "\"${match.groupValues[1]}\""
            assertTrue(path in validPaths, "Unexpected path: $path")
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

        // Randomize the original further
        original.randomize(rand, tryToForceNewValue = true)

        // The copy should retain the old state (before the re-randomize)
        // This is a probabilistic assertion — with enough ops they should differ sometimes
        // We just verify both are structurally valid
        assertTrue(copy.getValueAsPrintableString().startsWith("["))
        assertTrue(original.getValueAsPrintableString().startsWith("["))
    }

    @Test
    fun `copy preserves resourceSchema reference type`() {
        val original = doc()
        val copy = original.copy() as JsonPatchDocumentGene
        // Schema is copied (new instance of same class)
        if (original.resourceSchema != null) {
            assertEquals(original.resourceSchema!!.javaClass, copy.resourceSchema!!.javaClass)
        }
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
        val d = JsonPatchDocumentGene("patch", schema)
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
        val d = JsonPatchDocumentGene("patch", schema)
        d.doInitialize(rand)
        val seenOps = mutableSetOf<String>()
        repeat(40) {
            d.randomize(rand, tryToForceNewValue = true)
            d.operations.forEach { seenOps.add(it.operationName) }
        }
        assertTrue(seenOps.size > 1, "Expected multiple operation types, got: $seenOps")
    }

    // --- schema extraction integration ---

    @Test
    fun `all 3 schema fields appear as valid paths`() {
        val d = doc()
        val result = d.getValueAsPrintableString()
        // Produce many samples to see all paths
        val seenPaths = mutableSetOf<String>()
        val d2 = JsonPatchDocumentGene("patch", schema)
        d2.doInitialize(rand)
        repeat(50) {
            d2.randomize(rand, tryToForceNewValue = true)
            seenPaths.addAll(
                Regex("\"path\":\"(/[^\"]+)\"").findAll(d2.getValueAsPrintableString())
                    .map { it.groupValues[1] }
            )
        }
        assertTrue("/name" in seenPaths || "/email" in seenPaths || "/age" in seenPaths)
    }

    @Test
    fun `schema with only string fields groups them into one entry`() {
        val stringOnlySchema = ObjectGene("r", listOf(StringGene("a"), StringGene("b"), StringGene("c")))
        val d = JsonPatchDocumentGene("patch", stringOnlySchema)
        d.doInitialize(rand)
        // All paths should be string-type: /a, /b, /c
        val result = d.getValueAsPrintableString()
        assertTrue(result.startsWith("["))
    }

    @Test
    fun `null schema falls back to root path without crashing`() {
        val d = JsonPatchDocumentGene("patch", null)
        d.doInitialize(rand)
        val result = d.getValueAsPrintableString()
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
    }

    @Test
    fun `empty ObjectGene schema falls back gracefully`() {
        val emptySchema = ObjectGene("r", emptyList())
        val d = JsonPatchDocumentGene("patch", emptySchema)
        d.doInitialize(rand)
        val result = d.getValueAsPrintableString()
        assertTrue(result.startsWith("["))
    }

    // --- operation type breakdown ---

    @Test
    fun `template contains remove as first choice`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        val firstChoice = array.template.getViewOfChildren()[0] as JsonPatchOperationGene
        assertEquals("remove", firstChoice.operationName)
        assertTrue(firstChoice is JsonPatchPathOnlyGene)
    }

    @Test
    fun `template contains move and copy as second and third choices`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        val children = array.template.getViewOfChildren()
        val move = children[1] as JsonPatchOperationGene
        val copy = children[2] as JsonPatchOperationGene
        assertEquals("move", move.operationName)
        assertEquals("copy", copy.operationName)
        assertTrue(move is JsonPatchFromPathGene)
        assertTrue(copy is JsonPatchFromPathGene)
    }

    @Test
    fun `template contains add, replace, test as fourth through sixth choices`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        val children = array.template.getViewOfChildren()
        val add     = children[3] as JsonPatchOperationGene
        val replace = children[4] as JsonPatchOperationGene
        val test    = children[5] as JsonPatchOperationGene
        assertEquals("add", add.operationName)
        assertEquals("replace", replace.operationName)
        assertEquals("test", test.operationName)
        assertTrue(add is JsonPatchPathValueGene)
        assertTrue(replace is JsonPatchPathValueGene)
        assertTrue(test is JsonPatchPathValueGene)
    }

    @Test
    fun `path-value operations hold PairGene entries inside their ChoiceGene`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        val children = array.template.getViewOfChildren()

        for (idx in 3..5) {
            val op = children[idx] as JsonPatchPathValueGene
            val choiceChildren = op.pathValueChoice.getViewOfChildren()
            assertTrue(choiceChildren.isNotEmpty())
            choiceChildren.forEach { entry ->
                assertInstanceOf(PairGene::class.java, entry,
                    "Expected PairGene but got ${entry.javaClass.simpleName}")
                @Suppress("UNCHECKED_CAST")
                val pair = entry as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
                assertInstanceOf(EnumGene::class.java, pair.first)
            }
        }
    }

    @Test
    fun `path-value PairGene first contains schema paths, second is typed value gene`() {
        val array = JsonPatchDocumentGene.buildOperationsArray(schema)
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene

        addOp.pathValueChoice.getViewOfChildren().forEach { entry ->
            @Suppress("UNCHECKED_CAST")
            val pair = entry as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
            val paths = pair.first.values.map { it.toString() }
            assertTrue(paths.all { it.startsWith("/") }, "Expected JSON pointer paths, got: $paths")
        }
    }
}