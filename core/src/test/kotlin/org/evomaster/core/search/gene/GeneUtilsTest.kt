package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GeneUtilsTest{

    @Test
    fun testPadding(){

        val x = 9
        val res = GeneUtils.padded(x, 2)

        assertEquals("09", res)
    }


    @Test
    fun testPaddingNegative(){

        val x = -2
        val res = GeneUtils.padded(x, 3)

        assertEquals("-02", res)
    }
}