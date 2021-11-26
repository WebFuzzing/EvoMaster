package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
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
}