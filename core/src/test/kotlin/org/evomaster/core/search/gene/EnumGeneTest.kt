package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.EnumGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnumGeneTest {

    @Test
    fun testEmptyDataList(){
        val data = listOf<Int>()
        val enumGene = EnumGene("value", data)
        assertEquals(0, enumGene.values.size)
    }
}