package org.evomaster.core.search.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MapGeneTest{

    @Test
    fun test(){
        val randomness = Randomness()
        val map = mutableMapOf("x" to 0.5.toFloat(), "y" to 0.3.toFloat(), "z" to 0.2.toFloat())
        var x = 0.0
        var y = 0.0
        var z = 0.0
        val runs = 10000
        for (i in runs downTo 0 step 1){
            when (randomness.chooseByProbability(map)){
                "x" -> x += 1.0
                "y" -> y += 1.0
                "z" -> z += 1.0
            }
        }

        assertTrue((x/runs) >= 0.48 && (x/runs) <= 0.52)
        assertTrue((y/runs) >= 0.28 && (y/runs) <= 0.32)
        assertTrue((z/runs) >= 0.18 && (z/runs) <= 0.22)
    }
}