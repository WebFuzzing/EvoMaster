package org.evomaster.core.search.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RandomnessTest{

    private lateinit var rand : Randomness

    private val seed = 42L

    @BeforeEach
    fun init(){
        rand = Randomness()
        rand.updateSeed(seed)
    }


    @Test
    fun testMaxMinInt(){

        val min = -42
        val max = 1234

        rand.updateSeed(seed)
        var a = ""
        for(i in 0 until 100){
            val k = rand.nextInt(min, max)
            assertTrue(k >= min)
            assertTrue(k <= max)
            a += k
        }

        rand.updateSeed(seed)
        var b = ""
        for(i in 0 until 100){
            val k = rand.nextInt(min, max)
            assertTrue(k >= min)
            assertTrue(k <= max)
            b += k
        }

        assertEquals(a, b)
    }


    @Test
    fun testChooseByProbability(){

        val m = mutableMapOf<String, Float>()
        m["a"] = 1f
        m["b"] = 2f
        m["c"] = 1f
        m["d"] = 1f
        m["e"] = 2f


        rand.updateSeed(seed)
        var a = ""
        for(i in 0 until 100){
            val k = rand.chooseByProbability(m)
            a += k
        }

        rand.updateSeed(seed)
        var b = ""
        for(i in 0 until 100){
            val k = rand.chooseByProbability(m)
            b += k
        }

        assertEquals(a, b)
    }


    @Test
    fun testChooseListUpToN(){

        val list = listOf(0,1,2,3,4,5,6,7,8,9,10)

        rand.updateSeed(seed)
        var a = ""
        for(i in 0 until 100){
            val k = rand.choose(list, 5)
            assertTrue(k.size >= 0, "Size is ${k.size}")
            assertTrue(k.size <= 5, "Size is ${k.size}")
            a += k.joinToString("")
        }

        rand.updateSeed(seed)
        var b = ""
        for(i in 0 until 100){
            val k = rand.choose(list, 5)
            assertTrue(k.size >= 0, "Size is ${k.size}")
            assertTrue(k.size <= 5, "Size is ${k.size}")
            b += k.joinToString("")
        }

        assertEquals(a, b)
    }


    @Test
    fun testChooseSetUpToN(){

        val list = setOf(0,1,2,3,4,5,6,7,8,9,10)

        rand.updateSeed(seed)
        var a = ""
        for(i in 0 until 100){
            val k = rand.choose(list, 5)
            assertTrue(k.size >= 0, "Size is ${k.size}")
            assertTrue(k.size <= 5, "Size is ${k.size}")
            a += k.joinToString("")
        }

        rand.updateSeed(seed)
        var b = ""
        for(i in 0 until 100){
            val k = rand.choose(list, 5)
            assertTrue(k.size >= 0, "Size is ${k.size}")
            assertTrue(k.size <= 5, "Size is ${k.size}")
            b += k.joinToString("")
        }

        assertEquals(a, b)
    }

    @Test
    fun testChooseFromMap(){

        val m = mutableMapOf<String, String>()
        m["a"] = "0"
        m["b"] = "1"
        m["c"] = "2"
        m["d"] = "3"
        m["e"] = "4"


        rand.updateSeed(seed)
        var a = ""
        for(i in 0 until 100){
            val k = rand.choose(m)
            a += k
        }

        rand.updateSeed(seed)
        var b = ""
        for(i in 0 until 100){
            val k = rand.choose(m)
            b += k
        }

        assertEquals(a, b)
    }
}