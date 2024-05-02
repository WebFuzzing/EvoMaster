package org.evomaster.core.search.service

import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DataPoolTest{

    private var pool = createPool()


    private fun createPool(threshold: Int? = null) : DataPool{
        val config = EMConfig()
        if(threshold!=null){
            config.thresholdDistanceForDataPool = threshold
        }
        return DataPool(config, Randomness())
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


    @Test
    fun testDistance(){

        val key = "id"
        val data = "123"
        pool.addValue(key, data)

        val res = pool.extractValue("id")
        assertEquals(data, res)

        val res1 = pool.extractValue("1id")
        assertEquals(data, res1)

        val res2 = pool.extractValue("1i2d")
        assertEquals(data, res2)

        val res3 = pool.extractValue("1i2d3")
        assertNull(res3)
    }


    @Test
    fun testSubstring(){

        pool = createPool(0)

        val key = "id"
        val data = "123"
        pool.addValue(key, data)

        val res = pool.extractValue("petId")
        assertEquals(data, res)

        val res1 = pool.extractValue("  ID 1232 d")
        assertEquals(data, res1)

        val res2 = pool.extractValue("i d")
        assertNull(res2)
    }
}