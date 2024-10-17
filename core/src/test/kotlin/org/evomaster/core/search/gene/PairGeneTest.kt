package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}