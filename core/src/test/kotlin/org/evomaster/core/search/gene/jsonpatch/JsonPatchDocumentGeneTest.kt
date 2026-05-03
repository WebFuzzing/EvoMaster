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
    fun testConstructionSucceeds() {
        val d = JsonPatchDocumentGene("patch")
        assertEquals("patch", d.name)
    }

    @Test
    fun testHasExactlyOneChild() {
        assertEquals(1, JsonPatchDocumentGene("patch").getViewOfChildren().size)
    }

    // --- operations property ---

    @Test
    fun testOperationsIsNonEmptyAfterInitialization() {
        assertTrue(doc().operations.isNotEmpty())
    }

    @Test
    fun testOperationsAllHaveValidOperationNames() {
        val valid = setOf("add", "remove", "replace", "move", "copy", "test")
        assertTrue(doc().operations.all { it.operationName in valid })
    }

    @Test
    fun testOperationsSizeRespectsMinSize() {
        assertTrue(doc().operations.size >= JsonPatchDocumentGene.MIN_SIZE)
    }

    @Test
    fun testOperationsSizeRespectsMaxSize() {
        assertTrue(doc().operations.size <= JsonPatchDocumentGene.DEFAULT_MAX_SIZE)
    }

    // --- getValueAsPrintableString ---

    @Test
    fun testOutputStartsWithOpeningBracket() {
        assertTrue(doc().getValueAsPrintableString().startsWith("["))
    }

    @Test
    fun testOutputEndsWithClosingBracket() {
        assertTrue(doc().getValueAsPrintableString().endsWith("]"))
    }

    @Test
    fun testEachOperationInOutputIsJsonObject() {
        val result = doc().getValueAsPrintableString()
        val objectPattern = Regex("""\{"op":"[^"]+"""")
        assertTrue(objectPattern.containsMatchIn(result), "Expected at least one JSON object in: $result")
    }

    @Test
    fun testEachOperationContainsOpField() {
        val d = doc()
        val result = d.getValueAsPrintableString()
        val opCount = result.split("\"op\"").size - 1
        assertEquals(d.operations.size, opCount)
    }

    @Test
    fun testPathFieldValuesAreDoubleQuoted() {
        val d = doc()
        repeat(20) {
            d.randomize(rand, tryToForceNewValue = true)
            val result = d.getValueAsPrintableString()
            assertFalse(result.contains("\"path\":/"),
                "Found unquoted path value in: $result")
            val quoted = Regex("\"path\":\"(/[^\"]*?)\"").findAll(result).toList()
            assertTrue(quoted.isNotEmpty(), "No properly quoted path found in: $result")
        }
    }

    @Test
    fun testFromFieldValuesAreDoubleQuoted() {
        val d = doc()
        repeat(20) {
            d.randomize(rand, tryToForceNewValue = true)
            val result = d.getValueAsPrintableString()
            assertFalse(result.contains("\"from\":/"),
                "Found unquoted from value in: $result")
        }
    }

    @Test
    fun testRemoveOperationSerializesToExactJsonFormat() {
        val op = JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/x")))
        val result = op.getValueAsPrintableString()
        assertEquals("{\"op\":\"remove\",\"path\":\"/x\"}", result)
    }

    @Test
    fun testMoveOperationSerializesToExactJsonFormat() {
        val op = JsonPatchFromPathGene(
            "move", "move",
            fromGene = EnumGene("from", listOf("/a")),
            pathGene  = EnumGene("path", listOf("/b"))
        )
        val result = op.getValueAsPrintableString()
        assertEquals("{\"op\":\"move\",\"from\":\"/a\",\"path\":\"/b\"}", result)
    }

    @Test
    fun testAddOperationSerializesToExactJsonFormat() {
        @Suppress("UNCHECKED_CAST")
        val op = JsonPatchPathValueGene(
            "add", "add",
            org.evomaster.core.search.gene.wrapper.ChoiceGene("addPathValue", listOf(
                PairGene(
                    "e",
                    EnumGene("path", listOf("/name")),
                    StringGene("value", "Alice")
                ) as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
            ))
        )
        val result = op.getValueAsPrintableString()
        assertEquals("{\"op\":\"add\",\"path\":\"/name\",\"value\":\"Alice\"}", result)
    }

    // --- copy ---

    @Test
    fun testCopyProducesSameStringOutput() {
        val original = doc()
        val copy = original.copy() as JsonPatchDocumentGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
    }

    @Test
    fun testCopyIsIndependentFromOriginal() {
        val original = doc()
        val copy = original.copy() as JsonPatchDocumentGene
        original.randomize(rand, tryToForceNewValue = true)
        assertTrue(copy.getValueAsPrintableString().startsWith("["))
        assertTrue(original.getValueAsPrintableString().startsWith("["))
    }

    // --- containsSameValueAs ---

    @Test
    fun testContainsSameValueAsTrueForCopies() {
        val d1 = doc()
        val d2 = d1.copy() as JsonPatchDocumentGene
        assertTrue(d1.containsSameValueAs(d2))
    }

    @Test
    fun testContainsSameValueAsThrowsForWrongType() {
        assertThrows<IllegalArgumentException> {
            doc().containsSameValueAs(StringGene("x"))
        }
    }

    // --- randomize ---

    @Test
    fun testMultipleRandomizeProduceValidJsonArrays() {
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
    fun testRandomizeProducesDiverseOperationNames() {
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
    fun testTemplateContains6Choices() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertEquals(6, array.template.getViewOfChildren().size)
    }

    @Test
    fun testTemplateRespectsMinAndMaxSize() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        assertEquals(JsonPatchDocumentGene.MIN_SIZE, array.minSize)
        assertEquals(JsonPatchDocumentGene.DEFAULT_MAX_SIZE, array.maxSize)
    }

    @Test
    fun testPathValueOperationsHoldPairGeneEntries() {
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
    fun testPathValueEntrySecondIsStringGene() {
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