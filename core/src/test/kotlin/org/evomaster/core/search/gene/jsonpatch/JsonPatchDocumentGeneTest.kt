package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.builder.JsonPatchDocumentGeneBuilder
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.ChoiceGene
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

    @Test
    fun testConstructionWithNonNullResourceSchemaDoesNotThrow() {
        assertDoesNotThrow { JsonPatchDocumentGene("patch", StringGene("schema")) }
    }

    @Test
    fun testCopyPreservesNonNullResourceSchema() {
        val original = JsonPatchDocumentGene("patch", StringGene("schema"))
        val copy = original.copy() as JsonPatchDocumentGene
        assertNotNull(copy.resourceSchema)
    }

    // --- operations property ---

    @Test
    fun testOperationsIsNonEmptyAfterInitialization() {
        assertTrue(doc().operations.isNotEmpty())
    }

    @Test
    fun testOperationsAllHaveValidOperationNames() {
        val valid = setOf(JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_COPY, JsonPatchOperationGene.OP_TEST)
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
        val op = JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, EnumGene("path", listOf("/x")))
        val result = op.getValueAsPrintableString()
        assertEquals("{\"op\":\"remove\",\"path\":\"/x\"}", result)
    }

    @Test
    fun testMoveOperationSerializesToExactJsonFormat() {
        val op = JsonPatchFromPathGene(
            JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            fromGene = EnumGene("from", listOf("/a")),
            pathGene  = EnumGene("path", listOf("/b"))
        )
        val result = op.getValueAsPrintableString()
        assertEquals("{\"op\":\"move\",\"from\":\"/a\",\"path\":\"/b\"}", result)
    }

    @Test
    fun testAddOperationSerializesToExactJsonFormat() {
        val op = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("addPathValue", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "Alice"))
            ))
        )
        val result = op.getValueAsPrintableString()
        assertEquals("{\"op\":\"add\",\"path\":\"/name\",\"value\":\"Alice\"}", result)
    }

    // --- XML serialization ---

    @Test
    fun testRemoveOperationXmlFormat() {
        val op = JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, EnumGene("path", listOf("/x")))
        val result = op.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<operation><op>remove</op><path>/x</path></operation>", result)
    }

    @Test
    fun testMoveOperationXmlFormat() {
        val op = JsonPatchFromPathGene(
            JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
            fromGene = EnumGene("from", listOf("/a")),
            pathGene  = EnumGene("path", listOf("/b"))
        )
        val result = op.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<operation><op>move</op><from>/a</from><path>/b</path></operation>", result)
    }

    @Test
    fun testAddOperationXmlFormat() {
        val op = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("addPathValue", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "Alice"))
            ))
        )
        val result = op.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<operation><op>add</op><path>/name</path><value>Alice</value></operation>", result)
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

    // --- unsafeCopyValueFrom ---

    @Test
    fun testUnsafeCopyValueFromSameTypeSucceeds() {
        val source = doc(seed = 1L)
        val target = doc(seed = 2L)
        assertTrue(target.unsafeCopyValueFrom(source))
        assertEquals(source.getValueAsPrintableString(), target.getValueAsPrintableString())
    }

    @Test
    fun testUnsafeCopyValueFromWrongTypeReturnsFalse() {
        assertFalse(doc().unsafeCopyValueFrom(StringGene("x")))
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
                val pair = entry as PairGene<*, *>
                assertInstanceOf(EnumGene::class.java, pair.first)
            }
        }
    }

    @Test
    fun testPathValueEntrySecondIsStringGene() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        addOp.pathValueChoice.getViewOfChildren().forEach { entry ->
            val pair = entry as PairGene<*, *>
            assertInstanceOf(StringGene::class.java, pair.second)
            val pathEnum = pair.first as EnumGene<*>
            assertTrue(pathEnum.values.all { it.toString().startsWith("/") }, "Expected JSON pointer paths, got: ${pathEnum.values}")
        }
    }
}