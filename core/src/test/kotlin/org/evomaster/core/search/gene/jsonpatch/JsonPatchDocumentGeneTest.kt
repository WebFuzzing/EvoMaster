package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.builder.JsonPatchDocumentGeneBuilder
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
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

    @Test
    fun testContainsSameValueAsTrueWhenBothSchemasAreNull() {
        val d1 = doc()
        val d2 = d1.copy() as JsonPatchDocumentGene
        assertNull(d1.resourceSchema)
        assertNull(d2.resourceSchema)
        assertTrue(d1.containsSameValueAs(d2))
    }

    @Test
    fun testContainsSameValueAsFalseWhenOneSchemaIsNull() {
        val withSchema = JsonPatchDocumentGene("patch", ObjectGene("body", listOf(StringGene("name"))))
        withSchema.doInitialize(Randomness().apply { updateSeed(42) })
        val withoutSchema = doc()
        assertFalse(withSchema.containsSameValueAs(withoutSchema))
        assertFalse(withoutSchema.containsSameValueAs(withSchema))
    }

    @Test
    fun testContainsSameValueAsTrueForCopiesWithSchema() {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val d1 = JsonPatchDocumentGene("patch", schema)
        d1.doInitialize(Randomness().apply { updateSeed(1) })
        val d2 = d1.copy() as JsonPatchDocumentGene
        assertTrue(d1.containsSameValueAs(d2))
    }

    @Test
    fun testContainsSameValueAsFalseForDifferentSchemaTypes() {
        val schemaA = ObjectGene("body", listOf(StringGene("name")))
        val schemaB = ObjectGene("body", listOf(IntegerGene("count")))
        val d1 = JsonPatchDocumentGene("patch", schemaA)
        d1.doInitialize(Randomness().apply { updateSeed(1) })
        val d2 = JsonPatchDocumentGene("patch", schemaB)
        d2.doInitialize(Randomness().apply { updateSeed(1) })
        assertFalse(d1.containsSameValueAs(d2))
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

    // --- constructor with randomness ---

    @Test
    fun testRandomnessConstructorProducesValidDoc() {
        val rand = Randomness().apply { updateSeed(77) }
        val d = JsonPatchDocumentGene("patch", randomness = rand)
        d.doInitialize(rand)
        val result = d.getValueAsPrintableString()
        assertTrue(result.startsWith("[") && result.endsWith("]"),
            "Doc built with randomness must still be a valid JSON array: $result")
    }

    @Test
    fun testRandomnessConstructorPathsDifferFromDefaults() {
        val rand = Randomness().apply { updateSeed(77) }
        val d = JsonPatchDocumentGene("patch", randomness = rand)
        // inspect the template's remove operation path enum before doInitialize
        val array = d.getViewOfChildren()[0] as org.evomaster.core.search.gene.collection.ArrayGene<*>
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as org.evomaster.core.search.gene.collection.EnumGene<*>).values.map { it.toString() }
        assertTrue(paths.isNotEmpty() && paths.all { it.startsWith("/") },
            "Random paths must be non-empty and start with '/': $paths")
    }

    // --- full document print (JSON and XML) ---

    @Test
    fun testFullDocumentJsonPrint() {
        val d = doc()
        val result = d.getValueAsPrintableString()

        // outer array brackets
        assertTrue(result.startsWith("["), "Expected '[' at start, got: $result")
        assertTrue(result.endsWith("]"), "Expected ']' at end, got: $result")

        // strip brackets and split on operation boundaries to get individual objects
        val inner = result.removeSurrounding("[", "]")
        val opPattern = Regex("""\{"op":"[^"]+"""")
        val matches = opPattern.findAll(inner).toList()
        assertEquals(d.operations.size, matches.size,
            "Number of {\"op\":...} objects should equal operations.size. Got: $result")

        // every element must be a JSON object (starts with { ends with })
        val elementPattern = Regex("""^\[(\{"op":"[^"]+"[^}]*\})(, \{"op":"[^"]+"[^}]*\})*\]$""")
        assertTrue(elementPattern.containsMatchIn(result),
            "Full document JSON should be [ {op1}, {op2}, ... ], got: $result")
    }

    @Test
    fun testFullDocumentXmlPrint() {
        val d = doc()
        val result = d.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)

        // must have a single <patch> root element
        assertTrue(result.startsWith("<patch>"), "XML output must start with '<patch>', got: $result")
        assertTrue(result.endsWith("</patch>"), "XML output must end with '</patch>', got: $result")

        // every operation must be wrapped in <operation>...</operation>
        val tagPattern = Regex("""<operation><op>[^<]+</op>.*?</operation>""", RegexOption.DOT_MATCHES_ALL)
        val matches = tagPattern.findAll(result).toList()
        assertEquals(d.operations.size, matches.size,
            "Number of <operation> tags should equal operations.size. Got: $result")

        // the inner content must be exactly the concatenation of operation tags
        val inner = result.removeSurrounding("<patch>", "</patch>")
        val fullPattern = Regex("""^(<operation><op>[^<]+</op>.*?</operation>)+$""", RegexOption.DOT_MATCHES_ALL)
        assertTrue(fullPattern.containsMatchIn(inner),
            "Inner XML should be <operation>...</operation>..., got: $inner")
    }

    // --- template structure (delegated to builder; sanity checks here) ---

    @Test
    fun testTemplateContains6Choices() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        assertEquals(6, array.template.getViewOfChildren().size)
    }

    @Test
    fun testTemplateRespectsMinAndMaxSize() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        assertEquals(JsonPatchDocumentGene.MIN_SIZE, array.minSize)
        assertEquals(JsonPatchDocumentGene.DEFAULT_MAX_SIZE, array.maxSize)
    }

    @Test
    fun testPathValueOperationsHoldPairGeneEntries() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
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
    fun testPathValueEntriesIncludeStringAndIntegerGene() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entries = addOp.pathValueChoice.getViewOfChildren().map { it as PairGene<*, *> }
        assertTrue(entries.any { it.second is StringGene },  "Expected a StringGene value entry")
        assertTrue(entries.any { it.second is IntegerGene }, "Expected an IntegerGene value entry")
        entries.forEach { pair ->
            val pathEnum = pair.first as EnumGene<*>
            assertTrue(pathEnum.values.all { it.toString().startsWith("/") },
                "Expected JSON pointer paths, got: ${pathEnum.values}")
        }
    }
}