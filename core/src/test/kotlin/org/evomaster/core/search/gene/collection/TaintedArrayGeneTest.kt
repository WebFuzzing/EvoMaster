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

    private fun resolved(taintId: Int = 1): TaintedArrayGene =
        TaintedArrayGene("resolved", TaintInputName.getTaintName(taintId), true, ArrayGene("array", IntegerGene("element")))

    private fun unresolved(taintId: Int = 2): TaintedArrayGene =
        TaintedArrayGene("unresolved", TaintInputName.getTaintName(taintId))

    @Test
    fun testCopyBetweenResolvedGenes() {
        val target = resolved().apply { doInitialize(randomness) }
        val source = resolved().apply { doInitialize(randomness) }
        source.randomize(randomness, true)

        assertTrue(target.copyValueFrom(source))
        assertTrue(target.containsSameValueAs(source))
    }

    /**
     * This used to report a successful copy, even if the tainted value of the source was not copied.
     */
    @Test
    fun testCopyBetweenUnresolvedGenes() {
        val target = unresolved(2).apply { doInitialize(randomness) }
        val source = unresolved(3).apply { doInitialize(randomness) }

        assertTrue(target.copyValueFrom(source))
        assertEquals(source.taintedValue, target.taintedValue)
        assertTrue(target.containsSameValueAs(source))
    }

    /**
     * This used to throw a NullPointerException, as the missing array of the source was dereferenced.
     */
    @Test
    fun testCopyFromUnresolvedIntoResolved() {
        val target = resolved().apply { doInitialize(randomness) }
        val source = unresolved().apply { doInitialize(randomness) }

        assertTrue(target.copyValueFrom(source))
        assertFalse(target.isResolved())
        assertTrue(target.containsSameValueAs(source))
    }

    /**
     * This used to report a successful copy, even if the target was left without the array of the source.
     */
    @Test
    fun testCopyFromResolvedIntoUnresolved() {
        val target = unresolved().apply { doInitialize(randomness) }
        val source = resolved().apply { doInitialize(randomness) }

        assertTrue(target.copyValueFrom(source))
        assertTrue(target.isResolved())
        assertTrue(target.containsSameValueAs(source))
    }

    /**
     * A choice copies into its first alternative of the same class as the source, which here is in a
     * different state than the source. This used to throw a NullPointerException.
     */
    @Test
    fun testChoiceCopiesIntoAnAlternativeInADifferentState() {
        val choice = ChoiceGene<Gene>("choice", listOf(resolved(), unresolved()))
        choice.doInitialize(randomness)

        val target = choice.copy() as ChoiceGene<*>
        target.selectActiveGene(0)
        val source = choice.copy() as ChoiceGene<*>
        source.selectActiveGene(1)

        assertTrue(target.copyValueFrom(source))
        assertTrue(target.containsSameValueAs(source))
    }
}