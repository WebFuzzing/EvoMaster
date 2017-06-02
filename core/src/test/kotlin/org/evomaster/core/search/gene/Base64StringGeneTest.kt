package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class Base64StringGeneTest{

    @Test
    fun test(){

        val kotlin = "kotlin 123"
        val gene = Base64StringGene("gene", StringGene("data", kotlin))

        //checked with http://base64encode.net/
        val expected = "a290bGluIDEyMw=="

        assertEquals(expected, gene.getValueAsPrintableString())
    }
}

