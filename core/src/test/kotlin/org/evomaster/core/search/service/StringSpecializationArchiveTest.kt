package org.evomaster.core.search.service

import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StringSpecializationArchiveTest{

    @Test
    fun testSample(){

        val name = "bar"
        val x = StringSpecializationInfo(StringSpecialization.BOOLEAN, "")
        val y = StringSpecializationInfo(StringSpecialization.CONSTANT, "foo")

        val archive = StringSpecializationArchive()

        for(i in 0 until 99){
            archive.updateStats(name, x)
        }
        archive.updateStats(name, y)

        val rand = Randomness().apply { updateSeed(42) }
        val n = 10_000

        var px = 0.0
        var py = 0.0

        for(i in 0 until n){
            val k = archive.chooseSpecialization(name, rand)
            if(k == x) px += 1.0
            else if(k == y) py += 1.0
            else fail()
        }
        px = px / n
        py = py / n

        assertTrue(px >=0 && px <= 1, "px=$px")
        assertTrue(py >=0 && py <= 1, "py=$py")
        assertTrue(px < 0.3, "px=$px")
        assertTrue(py > 0.95, "py=$py")
    }

}