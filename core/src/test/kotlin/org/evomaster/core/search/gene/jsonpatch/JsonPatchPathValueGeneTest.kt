package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPatchPathValueGeneTest {

    private val rand = Randomness().apply { updateSeed(42) }

    private fun stringPair(path: String = "/name", value: String = "foo") =
        PairGene<EnumGene<String>, StringGene>("entry_str", EnumGene("path", listOf(path)), StringGene("value", value))

    private fun intPair(path: String = "/age", value: Int = 0) =
        PairGene<EnumGene<String>, IntegerGene>("entry_int", EnumGene("path", listOf(path)), IntegerGene("value", value))

    @Suppress("UNCHECKED_CAST")
    private fun addOp(vararg pairs: PairGene<EnumGene<String>, out org.evomaster.core.search.gene.Gene>): JsonPatchPathValueGene {
        val list: List<PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>> =
            if (pairs.isEmpty()) listOf(stringPair()) as List<PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>>
            else pairs.map { it as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene> }
        return JsonPatchPathValueGene("add", "add", ChoiceGene("addPathValue", list))
    }

    // --- Construction ---

    @Test
    fun testGeneNameEqualsOperationNameByDefault() {
        assertEquals("add", addOp().name)
        assertEquals("replace", JsonPatchPathValueGene("replace", "replace",
            ChoiceGene("x", listOf(stringPair() as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>))).name)
        assertEquals("test", JsonPatchPathValueGene("test", "test",
            ChoiceGene("x", listOf(stringPair() as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>))).name)
    }

    @Test
    fun testCustomGeneNameIsAccepted() {
        val gene = JsonPatchPathValueGene("myAdd", "add",
            ChoiceGene("x", listOf(stringPair() as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>)))
        assertEquals("myAdd", gene.name)
    }

    @Test
    fun testOperationNameIsStored() {
        assertEquals("add", addOp().operationName)
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
        @Suppress("UNCHECKED_CAST")
        val gene = JsonPatchPathValueGene(
            "add", "add",
            ChoiceGene("addPathValue", listOf(
                PairGene("e", EnumGene("path", listOf("/name")), StringGene("value", "Alice"))
                        as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
            ))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"add\",\"path\":\"/name\",\"value\":\"Alice\"}", result)
    }

    @Test
    fun testReplaceWithIntegerPairProducesValidJson() {
        @Suppress("UNCHECKED_CAST")
        val gene = JsonPatchPathValueGene(
            "replace", "replace",
            ChoiceGene("replacePathValue", listOf(
                PairGene("e", EnumGene("path", listOf("/age")), IntegerGene("value", 30))
                        as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
            ))
        )
        val result = gene.getValueAsPrintableString()
        assertEquals("{\"op\":\"replace\",\"path\":\"/age\",\"value\":30}", result)
    }

    @Test
    fun testOperationIsCorrectlyLabelled() {
        @Suppress("UNCHECKED_CAST")
        val gene = JsonPatchPathValueGene(
            "test", "test",
            ChoiceGene("testPathValue", listOf(
                PairGene("e", EnumGene("path", listOf("/name")), StringGene("value", "Bob"))
                        as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
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
        @Suppress("UNCHECKED_CAST")
        val choice = ChoiceGene("addPathValue", listOf(
            PairGene("e0", EnumGene("path", listOf("/name")), StringGene("value", "hello"))
                    as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>,
            PairGene("e1", EnumGene("path", listOf("/age")), IntegerGene("value", 25))
                    as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
        ))
        val gene = JsonPatchPathValueGene("add", "add", choice)

        choice.selectActiveGene(0)
        assertTrue(gene.getValueAsPrintableString().contains("\"value\":\"hello\""))

        choice.selectActiveGene(1)
        assertTrue(gene.getValueAsPrintableString().contains("\"value\":25"))
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
        @Suppress("UNCHECKED_CAST")
        val original = JsonPatchPathValueGene(
            "add", "add",
            ChoiceGene("x", listOf(
                PairGene("e", EnumGene("path", listOf("/name")), StringGene("value", "before"))
                        as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
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
            addOp().containsSameValueAs(JsonPatchPathOnlyGene("remove", "remove", EnumGene("path", listOf("/"))))
        }
    }

    // --- randomize ---

    @Test
    fun testRandomizeChangesValueContent() {
        @Suppress("UNCHECKED_CAST")
        val gene = JsonPatchPathValueGene(
            "add", "add",
            ChoiceGene("x", listOf(
                PairGene("e", EnumGene("path", listOf("/name")), StringGene("value", "init"))
                        as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
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
        @Suppress("UNCHECKED_CAST")
        val gene = JsonPatchPathValueGene(
            "add", "add",
            ChoiceGene("x", listOf(
                stringPair("/name") as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>,
                intPair("/age") as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
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
        @Suppress("UNCHECKED_CAST")
        val gene = JsonPatchPathValueGene(
            "add", "add",
            ChoiceGene("x", listOf(
                PairGene("e", EnumGene("path", listOf("/name")), StringGene("value", "hi"))
                        as PairGene<EnumGene<String>, org.evomaster.core.search.gene.Gene>
            ))
        )
        val pair = gene.pathValueChoice.activeGene()
        assertInstanceOf(EnumGene::class.java, pair.first)
        assertInstanceOf(StringGene::class.java, pair.second)
    }
}