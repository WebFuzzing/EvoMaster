package org.evomaster.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IncrementalAverageTest{

    @Test
    fun testBase(){

        val a = IncrementalAverage()
        a.addValue(1)
        a.addValue(2)
        a.addValue(3)

        assertEquals(3, a.n)
        assertEquals(2.0, a.mean, 0.01)
        assertEquals(1.0, a.min)
        assertEquals(3.0, a.max)
    }

    @Test
    fun testLarge(){

        val a = IncrementalAverage()
        val n = 100
        var sum = 0.0

        for(i in 0..n){
            sum += i
            a.addValue(i)
        }

        val res = a.n * a.mean

        assertEquals(sum, res, 0.01)
        assertEquals(0.0, a.min)
        assertEquals(n.toDouble(), a.max)
    }


    @Test
    fun testTimer(){

        val a = IncrementalAverage()

        a.doStartTimer()
        Thread.sleep(200)
        a.addElapsedTime()

        a.doStartTimer()
        Thread.sleep(500)
        a.addElapsedTime()

        assertTrue(a.min >= 200)
        assertTrue(a.max < 1000)
        assertTrue(a.mean  > 200 && a.mean < 600)
    }
}