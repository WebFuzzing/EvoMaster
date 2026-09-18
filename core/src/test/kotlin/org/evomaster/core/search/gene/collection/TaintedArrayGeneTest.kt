package org.evomaster.core.search.gene.collection

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TaintedArrayGeneTest {

    private val randomness = Randomness()

    private fun resolved(): TaintedArrayGene =
        TaintedArrayGene("resolved", TaintInputName.getTaintName(1), true, ArrayGene("array", IntegerGene("element")))

    private fun unresolved(): TaintedArrayGene =
        TaintedArrayGene("unresolved", TaintInputName.getTaintName(2))

    @Test
    fun testCopyBetweenResolvedGenes() {
        val target = resolved().apply { doInitialize(randomness) }
        val source = resolved().apply { doInitialize(randomness) }
        source.randomize(randomness, true)

        assertTrue(target.copyValueFrom(source))
        assertTrue(target.containsSameValueAs(source))
    }

    /**
     * This used to throw a NullPointerException, as the missing array of the source was dereferenced.
     */
    @Test
    fun testCopyFromUnresolvedIntoResolvedFails() {
        val target = resolved().apply { doInitialize(randomness) }
        val source = unresolved().apply { doInitialize(randomness) }
        val before = target.copy()

        assertFalse(target.copyValueFrom(source))
        assertTrue(target.isResolved())
        assertTrue(target.containsSameValueAs(before))
    }

    /**
     * This used to report a successful copy, even if the target was left without the array of the source.
     */
    @Test
    fun testCopyFromResolvedIntoUnresolvedFails() {
        val target = unresolved().apply { doInitialize(randomness) }
        val source = resolved().apply { doInitialize(randomness) }

        assertFalse(target.copyValueFrom(source))
        assertFalse(target.isResolved())
        assertFalse(target.containsSameValueAs(source))
    }

    /**
     * A choice has to copy into the alternative in the same state as the source, and not into the
     * first alternative of the same class. This used to throw a NullPointerException.
     */
    @Test
    fun testChoiceCopiesIntoTheAlternativeInTheSameState() {
        val choice = ChoiceGene<Gene>("choice", listOf(resolved(), unresolved()))
        choice.doInitialize(randomness)

        val target = choice.copy() as ChoiceGene<*>
        target.selectActiveGene(0)
        val source = choice.copy() as ChoiceGene<*>
        source.selectActiveGene(1)

        assertTrue(target.copyValueFrom(source))
        assertEquals(1, target.activeGeneIndex)
        assertTrue(target.containsSameValueAs(source))
    }
}