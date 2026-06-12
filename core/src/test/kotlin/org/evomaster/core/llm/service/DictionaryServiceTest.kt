package org.evomaster.core.llm.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DictionaryServiceTest {


    @Test
    fun testLoadAll(){

        val data = DictionaryService.loadAll()
        assertEquals(66743, data.size)
        assertTrue(data.contains("abstract"))
    }


    @Test
    fun testSearchForNames(){

        val data = listOf("aaaa","abstract","abcd","acceptheader","xxxx")

        val result = DictionaryService.searchForNames(data)
        assertEquals(2, result.data.size)
        assertEquals(3, result.missing.size)
        assertTrue(result.data.contains("abstract"))
        assertTrue(result.missing.contains("abcd"))
        assertTrue(result.missing.contains("xxxx"))
    }


}