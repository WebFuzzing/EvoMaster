package org.evomaster.core.search.gene.regex

import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.regex.Pattern

class BackReferenceRxGeneTest {

    @Test
    fun testWithoutRegexGeneTree() {
        // nothing to read the value of the group from, but it must not fail
        val gene = BackReferenceRxGene(1, false)
        assertEquals("", gene.getValueAsRawString())
        assertEquals("", gene.copy().getValueAsRawString())
    }

    @Test
    fun testUnsatisfiableGroup() {
        val missing = BackReferenceRxGene(2, true)
        assertTrue(missing.isUnsatisfiable())
        assertThrows<IllegalStateException> { missing.getValueAsRawString() }
        // a copy of it is as unsatisfiable as the original
        assertTrue((missing.copy() as BackReferenceRxGene).isUnsatisfiable())

        assertFalse(BackReferenceRxGene(1, false).isUnsatisfiable())
    }

    @Test
    fun testGroupsAreOrderedCorrectly() {
        val regex = """^((a)(b))\3\2\1$"""
        val gene = RegexHandler.createGeneForJVM(regex)
        gene.randomize(Randomness().apply { updateSeed(42) }, false)
        // \3 is b, \2 is a, and \1 is ab
        assertEquals("abbaab", gene.getValueAsRawString())
        assertTrue(Pattern.compile(regex).matcher("abbaab").matches())
    }

    @Test
    fun testBackreferenceOfCachedCopy() {
        val regex = """^(\w{2})\1randomTextToAvoidCacheMatchBecauseOfRepeatedRegex$"""
        val randomness = Randomness().apply { updateSeed(42) }

        val g = RegexHandler.createGeneForJVM(regex)
        g.randomize(randomness, false)

        val g2 = RegexHandler.createGeneForJVM(regex) // this would be a copy of the cached value (g)
        repeat(20) {
            g2.randomize(randomness, false)
        }

        assertTrue(Pattern.compile(regex).matcher(g2.getValueAsRawString()).find())
    }

    @Test
    fun testNoDependencyBetweenCopyAndOriginal() {
        listOf("""^([a-z])\1txt98$""", """^(?<w>[a-z]{2})-\k<w>txt81$""", """^([a-z])(\d)\2\1txt92$""", """^((\d)([a-z]))\3\2\1txt37$""")
            .forEach { regex ->
                val pattern = Pattern.compile(regex)
                val randomness = Randomness().apply { updateSeed(42) }

                val original = RegexHandler.createGeneForJVM(regex)
                original.randomize(randomness, false)
                val valueOfOriginal = original.getValueAsRawString()

                val copy = original.copy() as RegexGene
                // same value, as it is a copy
                assertEquals(valueOfOriginal, copy.getValueAsRawString())

                repeat(50) {
                    copy.randomize(randomness, false)
                    assertTrue(pattern.matcher(copy.getValueAsRawString()).matches(), copy.getValueAsRawString())
                    // the original is not affected by what happens to its copy
                    assertEquals(valueOfOriginal, original.getValueAsRawString())
                }

                val valueOfCopy = copy.getValueAsRawString()

                repeat(50) {
                    original.randomize(randomness, false)
                    assertTrue(pattern.matcher(original.getValueAsRawString()).matches(), original.getValueAsRawString())
                    // the copy is not affected by what happens to the original
                    assertEquals(valueOfCopy, copy.getValueAsRawString())
                }
            }
    }

    @Test
    fun testGroupMutationModifiesBackReferenceValue() {
        // as back references are references to the captured group's value we expect mutations on the group to affect
        // the backref gene too
        val regex = """^([a-zA-Z]{8})\1$"""
        val matcher = Pattern.compile(regex).matcher("")

        val apc = AdaptiveParameterControl()
        val mwc = MutationWeightControl()

        val gene = RegexHandler.createGeneForJVM(regex)
        val randomness = Randomness().apply { updateSeed(42) }
        gene.doInitialize(randomness)

        repeat(20) {
            gene.randomize(randomness, false)
            val value = gene.getValueAsRawString()

            gene.standardMutation(randomness, apc = apc, mwc = mwc, childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DEFAULT)
            val mutatedValue = gene.getValueAsRawString()
            // rendering again does not give the same value as mutation changed the captured group
            assertNotEquals(value, mutatedValue)
            // both values should match the regex
            assert(matcher.reset(value).find())
            assert(matcher.reset(mutatedValue).find()) // the backref correctly gets the new group's value
        }
    }
}
