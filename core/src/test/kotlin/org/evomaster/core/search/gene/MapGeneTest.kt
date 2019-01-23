package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MapGeneTest{

    @Test
    fun test(){
        val s1 = StringGene("string_1")
        val s2 = StringGene("string_2")

        val map = MapGene("PrintableMap", StringGene("map"), 7, mutableListOf(s1, s2))
        val mapstring = map.getValueAsPrintableString()

        assertTrue(mapstring.contains(s1.getValueAsPrintableString(), ignoreCase = true))
        assertTrue(mapstring.contains(s2.getValueAsPrintableString(), ignoreCase = true))

    }
}