package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * created by manzhang on 2021/11/26
 */
class PairGeneTest {

    @Test
    fun test(){
        val intGene = IntegerGene("int1", 1)
        val stringGene = StringGene("str2", "foo")

        val pairGene = PairGene("pair", intGene, stringGene)

        assertEquals(
                "1:\"foo\"",
                pairGene.getValueAsPrintableString()
        )
    }

    @Test
    fun testMutableAndCopy(){
        val intGene = IntegerGene("int1", 1)
        val pairGene = PairGene.createStringPairGene(intGene, true)

        assertFalse(pairGene.allowedToMutateFirst)

        val copy = pairGene.copy() as PairGene<StringGene, IntegerGene>
        assertFalse(copy.allowedToMutateFirst)
    }

    @Test
    fun testContainsSameValueAsHomogeneousAndMismatchedTypes() {
        val p1 = PairGene("pair", StringGene("k", "a"), IntegerGene("v", 1))
        val p2 = PairGene("pair", StringGene("k", "a"), IntegerGene("v", 1))
        val p3 = PairGene("pair", StringGene("k", "a"), IntegerGene("v", 2))
        val diffFirst = PairGene("pair", IntegerGene("k", 1), IntegerGene("v", 1))
        val diffSecond = PairGene("pair", StringGene("k", "a"), StringGene("v", "1"))

        assertTrue(p1.containsSameValueAs(p2))
        assertFalse(p1.containsSameValueAs(p3))
        assertFalse(p1.containsSameValueAs(diffFirst))
        assertFalse(p1.containsSameValueAs(diffSecond))
    }
}
