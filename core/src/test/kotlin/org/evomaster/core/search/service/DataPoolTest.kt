package org.evomaster.core.search.service

import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DataPoolTest{

    private var pool = createPool()


    private fun createPool() : DataPool{
        return DataPool(EMConfig(), Randomness())
    }

    @BeforeEach
    fun initTest(){
        pool = createPool()
    }

    @Test
    fun testEmpty(){
        val res = pool.extractValue("foo")
        assertNull(res)
    }


    @Test
    fun testExactMatch(){

        val key = "foo"
        val data = "123"
        pool.addValue("bar", "hello")
        pool.addValue(key, data)
        pool.addValue("x", "there")

        val res = pool.extractValue(key)
        assertEquals(data, res)
    }

    @Test
    fun testQualifier(){

        val key = "petId"
        val data = "123"
        pool.addValue("bar", "hello")
        pool.addValue(key, data)
        pool.addValue("x", "there")

        val res = pool.extractValue("id", "pets")
        assertEquals(data, res)
    }

}