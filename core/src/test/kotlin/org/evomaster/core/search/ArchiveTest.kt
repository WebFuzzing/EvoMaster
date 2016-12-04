package org.evomaster.core.search

import org.evomaster.core.search.onemax.OneMaxFitness
import org.evomaster.core.search.onemax.OneMaxIndividual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach


class ArchiveTest{

    lateinit var archive: Archive<OneMaxIndividual>
    lateinit var ff : OneMaxFitness

    @BeforeEach
    fun init(){
        archive = Archive<OneMaxIndividual>()
        ff =  OneMaxFitness()
    }

    @Test
    fun testEmpty(){

        val archive = Archive<OneMaxIndividual>()

        val solution = archive.extractSolution()

        assertEquals(0.0, solution.overall.computeFitnessScore(), 0.001)
    }

    @Test
    fun testOneElement(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)

        val added = archive.addIfNeeded(ff.calculateCoverage(a))
        assertTrue(added)

        val solution = archive.extractSolution()

        assertEquals(1.0, solution.overall.computeFitnessScore(), 0.001)
        assertEquals(1, solution.individuals.size)
    }

    @Test
    fun testSecondElementNotNecessary(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(a))

        val b = OneMaxIndividual(2)
        b.setValue(1, 0.5)
        val added = archive.addIfNeeded(ff.calculateCoverage(b))
        assertTrue(added)

        val solution = archive.extractSolution()

        //b should had been ignored
        assertEquals(1.0, solution.overall.computeFitnessScore(), 0.001)
        assertEquals(1, solution.individuals.size)
    }

    @Test
    fun testTwoElements(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(a))

        val b = OneMaxIndividual(2)
        b.setValue(1, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(b))

        val solution = archive.extractSolution()

        assertEquals(2.0, solution.overall.computeFitnessScore(), 0.001)
        assertEquals(2, solution.individuals.size)
    }

}

