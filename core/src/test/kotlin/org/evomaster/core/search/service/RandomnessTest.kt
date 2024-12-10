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

        val m = mutableMapOf<String, Double>()
        m["a"] = 1.0
        m["b"] = 2.0
        m["c"] = 1.0
        m["d"] = 1.0
        m["e"] = 2.0


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

    @Test
    fun chooseIntRange(){

        val a = listOf(3,4,5,6)

        val seen = a.map { false }.toMutableList()

        for(i in 0 until 100){
            val k = rand.choose(a.indices)
            assertTrue(k in 0..3, "Wrong value: $k")
            seen[k] = true
        }
        assertTrue(seen.all { it })
    }


    @Test
    fun testProbability(){

        repeat(100){
            assertFalse(rand.nextBoolean(0.0))
        }

        repeat(100){
            assertTrue(rand.nextBoolean(1.0))
        }
    }
}