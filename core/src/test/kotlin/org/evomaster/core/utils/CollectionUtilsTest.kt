package org.evomaster.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CollectionUtilsTest{

    @Test
    fun testNoDuplicates(){

        val list = listOf("a","b","c")
        val duplicates = CollectionUtils.duplicates(list)
        assertEquals(0, duplicates.size)
    }

    @Test
    fun testDuplicates(){

        val list = listOf(1,1,1,0,2,3,3)
        val duplicates = CollectionUtils.duplicates(list)
        assertEquals(2, duplicates.size)
        assertEquals(3, duplicates[1])
        assertEquals(2, duplicates[3])
    }


}

