package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.Gene
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

class JsonPatchPathValueGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private fun stringPair(path: String = "/name", value: String = "foo"): PairGene<EnumGene<String>, Gene> =
        PairGene("entry_str", EnumGene("path", listOf(path)), StringGene("value", value))

    private fun intPair(path: String = "/age", value: Int = 0): PairGene<EnumGene<String>, Gene> =
        PairGene("entry_int", EnumGene("path", listOf(path)), IntegerGene("value", value))

    private fun addOp(vararg pairs: PairGene<EnumGene<String>, Gene>): JsonPatchPathValueGene {
        val list = if (pairs.isEmpty()) listOf(stringPair()) else pairs.toList()
        return JsonPatchPathValueGene(JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD, ChoiceGene("addPathValue", list))
    }

    // --- Construction ---

    @Test
    fun testGeneNameEqualsOperationNameByDefault() {
        assertEquals(JsonPatchOperationGene.OP_ADD, addOp().name)
        assertEquals(JsonPatchOperationGene.OP_REPLACE, JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_REPLACE,
            ChoiceGene("x", listOf(stringPair()))).name)
        assertEquals(JsonPatchOperationGene.OP_TEST, JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_TEST, JsonPatchOperationGene.OP_TEST,
            ChoiceGene("x", listOf(stringPair()))).name)
    }

    @Test
    fun testCustomGeneNameIsAccepted() {
        val gene = JsonPatchPathValueGene("myAdd", JsonPatchOperationGene.OP_ADD,
            ChoiceGene("x", listOf(stringPair())))
        assertEquals("myAdd", gene.name)
    }

    @Test
    fun testOperationNameIsStored() {
        assertEquals(JsonPatchOperationGene.OP_ADD, addOp().operationName)
    }

    @Test
    fun testHasExactlyOneChild() {
        val gene = addOp()
        assertEquals(1, gene.getViewOfChildren().size)
        assertSame(gene.pathValueChoice, gene.getViewOfChildren()[0])
    }

    // --- getValueAsPrintableString ---

    @Test
    fun testAddWithStringPairProducesValidJson() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("addPathValue", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "Alice"))
            ))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"add\",\"path\":\"/name\",\"value\":\"Alice\"}", result)
    }

    @Test
    fun testReplaceWithIntegerPairProducesValidJson() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_REPLACE,
            ChoiceGene("replacePathValue", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/age")), IntegerGene("value", 30))
            ))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"replace\",\"path\":\"/age\",\"value\":30}", result)
    }

    @Test
    fun testOperationIsCorrectlyLabelled() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_TEST, JsonPatchOperationGene.OP_TEST,
            ChoiceGene("testPathValue", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "Bob"))
            ))
        )
        assertTrue(gene.getValueAsPrintableString().startsWith("{\"op\":\"test\""))
    }

    @Test
    fun testOutputIsWrappedInBraces() {
        val result = addOp().getValueAsPrintableString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
    }

    @Test
    fun testOutputContainsRequiredFields() {
        val result = addOp().getValueAsPrintableString()
        assertTrue(result.contains("\"op\""))
        assertTrue(result.contains("\"path\""))
        assertTrue(result.contains("\"value\""))
    }

    @Test
    fun testActivePairDeterminesOutput() {
        val choice = ChoiceGene("addPathValue", listOf(
            PairGene<EnumGene<String>, Gene>("e0", EnumGene("path", listOf("/name")), StringGene("value", "hello")),
            PairGene<EnumGene<String>, Gene>("e1", EnumGene("path", listOf("/age")), IntegerGene("value", 25))
        ))
        val gene = JsonPatchPathValueGene(JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD, choice)

        choice.selectActiveGene(0)
        assertTrue(gene.getValueAsPrintableString().contains("\"value\":\"hello\""))

        choice.selectActiveGene(1)
        assertTrue(gene.getValueAsPrintableString().contains("\"value\":25"))
    }

    // --- XML serialization ---

    @Test
    fun testAddOutputInXmlMode() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("addPathValue", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "Alice"))
            ))
        )
        val result = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertTrue(result.startsWith("<operation><op>add</op>"))
        assertTrue(result.contains("<path>/name</path>"))
        assertTrue(result.contains("<value>"))
    }

    // --- copy ---

    @Test
    fun testCopyPreservesValues() {
        val original = addOp(stringPair("/name", "test"))
        val copy = original.copy() as JsonPatchPathValueGene
        assertEquals(original.operationName, copy.operationName)
        assertEquals(original.name, copy.name)
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
    }

    @Test
    fun testCopyIsIndependentFromOriginal() {
        val original = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("x", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "before"))
            ))
        )
        val copy = original.copy() as JsonPatchPathValueGene

        (original.pathValueChoice.activeGene().second as StringGene).value = "after"

        assertNotEquals(
            original.getValueAsPrintableString(),
            copy.getValueAsPrintableString()
        )
    }

    // --- containsSameValueAs ---

    @Test
    fun testContainsSameValueAsTrueForEqualPairs() {
        val a = addOp(stringPair("/name", "x"))
        val b = addOp(stringPair("/name", "x"))
        assertTrue(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsFalseWhenValuesDiffer() {
        val a = addOp(stringPair("/name", "x"))
        val b = addOp(stringPair("/name", "y"))
        assertFalse(a.containsSameValueAs(b))
    }

    @Test
    fun testContainsSameValueAsThrowsForWrongType() {
        assertThrows<IllegalArgumentException> {
            addOp().containsSameValueAs(JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, EnumGene("path", listOf("/"))))
        }
    }

    // --- unsafeCopyValueFrom ---

    @Test
    fun testUnsafeCopyValueFromSameTypeCopiesValue() {
        val source = addOp(stringPair("/name", "hello"))
        val target = addOp(stringPair("/name", "world"))
        assertTrue(target.unsafeCopyValueFrom(source))
        assertEquals(source.getValueAsPrintableString(), target.getValueAsPrintableString())
    }

    @Test
    fun testUnsafeCopyValueFromWrongTypeReturnsFalse() {
        assertFalse(addOp().unsafeCopyValueFrom(
            JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, EnumGene("path", listOf("/")))
        ))
    }

    @Test
    fun testUnsafeCopyValueFromDifferentOperationReturnsFalse() {
        val add = addOp()
        val replace = JsonPatchPathValueGene(JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_REPLACE,
            ChoiceGene("x", listOf(stringPair())))
        assertFalse(add.unsafeCopyValueFrom(replace))
    }

    // --- randomize ---

    @Test
    fun testRandomizeChangesValueContent() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("x", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "init"))
            ))
        )
        gene.doInitialize(rand)
        val seenValues = mutableSetOf<String>()
        repeat(20) {
            gene.randomize(rand, tryToForceNewValue = true)
            seenValues.add(gene.getValueAsPrintableString())
        }
        assertTrue(seenValues.size > 1, "Expected multiple randomized outputs, got: $seenValues")
    }

    @Test
    fun testRandomizeCanSwitchActivePair() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("x", listOf(
                stringPair("/name"),
                intPair("/age")
            ))
        )
        gene.doInitialize(rand)
        val seenActiveIndices = mutableSetOf<Int>()
        repeat(30) {
            gene.randomize(rand, tryToForceNewValue = true)
            seenActiveIndices.add(gene.pathValueChoice.activeGeneIndex)
        }
        assertTrue(seenActiveIndices.size > 1, "Expected ChoiceGene to switch active pair")
    }

    // --- isMutable ---

    @Test
    fun testIsMutableWhenValueGeneIsMutable() {
        assertTrue(addOp().isMutable())
    }

    // --- PairGene structure ---

    @Test
    fun testActivePairStructure() {
        val gene = JsonPatchPathValueGene(
            JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_ADD,
            ChoiceGene("x", listOf(
                PairGene<EnumGene<String>, Gene>("e", EnumGene("path", listOf("/name")), StringGene("value", "hi"))
            ))
        )
        val pair = gene.pathValueChoice.activeGene()
        assertInstanceOf(EnumGene::class.java, pair.first)
        assertInstanceOf(StringGene::class.java, pair.second)
    }
}