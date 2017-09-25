package org.evomaster.core.search.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.onemax.OneMaxFitness
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class ArchiveTest{

    private lateinit var archive: Archive<OneMaxIndividual>
    private lateinit var ff : OneMaxFitness
    private lateinit var config: EMConfig

    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
                .build().createInjector()


        archive = injector.getInstance(Key.get(
                object : TypeLiteral<Archive<OneMaxIndividual>>() {}))
        ff =  injector.getInstance(OneMaxFitness::class.java)
        config = injector.getInstance(EMConfig::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
    }

    @Test
    fun testEmpty(){

        val solution = archive.extractSolution()

        assertEquals(0.0, solution.overall.computeFitnessScore(), 0.001)
    }

    @Test
    fun testOneElement(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)

        val added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val solution = archive.extractSolution()

        assertEquals(1.0, solution.overall.computeFitnessScore(), 0.001)
        assertEquals(1, solution.individuals.size)
    }

    @Test
    fun testSecondElementNotNecessary(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(a)!!)

        val b = OneMaxIndividual(2)
        b.setValue(1, 0.5)
        val added = archive.addIfNeeded(ff.calculateCoverage(b)!!)
        assertTrue(added)

        val solution = archive.extractSolution()

        //b should had been ignored, as new target is not fully covered
        assertEquals(1.0, solution.overall.computeFitnessScore(), 0.001)
        assertEquals(1, solution.individuals.size)
    }

    @Test
    fun testTwoElements(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(a)!!)

        val b = OneMaxIndividual(2)
        b.setValue(1, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(b)!!)

        val solution = archive.extractSolution()

        assertEquals(2.0, solution.overall.computeFitnessScore(), 0.001)
        assertEquals(2, solution.individuals.size)
    }

    @Test
    fun testDontAdd(){

        val a = OneMaxIndividual(1)
        a.setValue(0, 0.0)
        val added = archive.addIfNeeded(ff.calculateCoverage(a)!!)

        //h=0 tests should not be added, as they give no contribution
        assertFalse(added)
    }

    @Test
    fun testSampleOnEmptyArchive(){
        try {
            archive.sampleIndividual()
            fail<Any>("Expected exception")
        }catch (e: Exception){}
    }

    @Test
    fun testSampleSimple(){

        val a = OneMaxIndividual(1)
        a.setValue(0, 0.25)
        val added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val sampled = archive.sampleIndividual()
        assertEquals(0.25, sampled.fitness.computeFitnessScore(), 0.0001)
    }

    @Test
    fun testSampleTwoOnSameTarget(){

        val a = OneMaxIndividual(1)
        a.setValue(0, 0.25)
        var added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val b = OneMaxIndividual(1)
        b.setValue(0, 0.5)
        added = archive.addIfNeeded(ff.calculateCoverage(b)!!)
        assertTrue(added)

        /*
            both are in the archive.
            Sampling 50 times should have nearly "p = 1 - (0.5)^50"
            of getting both of them, at least once
         */
        val n = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.fitness.computeFitnessScore()}
                .distinct()
                .count()

        assertEquals(2, n)
    }

    @Test
    fun testSampleTwoOnSameTargetWithShrinking(){

        val a = OneMaxIndividual(1)
        a.setValue(0, 0.25)
        var added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val b = OneMaxIndividual(1)
        b.setValue(0, 0.5)
        added = archive.addIfNeeded(ff.calculateCoverage(b)!!)
        assertTrue(added)

        //reduce buffer size, so "a" should disappear when sampling
        config.archiveTargetLimit = 1

        /*
            only one should be in archive
         */
        val n = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.fitness.computeFitnessScore()}
                .distinct()
                .count()

        assertEquals(1, n)

        val h = archive.sampleIndividual().fitness.computeFitnessScore()
        assertEquals(0.5, h, 0.001)
    }

    @Test
    fun testSampleTwoOnSameTargetWithCovered(){

        val a = OneMaxIndividual(1)
        a.setValue(0, 0.25)
        var added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val b = OneMaxIndividual(1)
        b.setValue(0, 1.0)
        added = archive.addIfNeeded(ff.calculateCoverage(b)!!)
        assertTrue(added)

        /*
            when "b" inserted, then "a" should had been removed
         */
        val n = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.fitness.computeFitnessScore()}
                .distinct()
                .count()

        assertEquals(1, n)

        val h = archive.sampleIndividual().fitness.computeFitnessScore()
        assertEquals(1.0, h, 0.001)
    }


    @Test
    fun testSampleTwoOnSameTargetWithShrinkingAndDifferentLength(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 0.25)
        var added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val b = OneMaxIndividual(1)
        b.setValue(0, 0.25)
        added = archive.addIfNeeded(ff.calculateCoverage(b)!!)
        assertTrue(added)

        //both should be sampled
        var n = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()
                .count()

        assertEquals(2, n)

        //reduce buffer size, so "a" should disappear when sampling, as longer
        config.archiveTargetLimit = 1

        //  only one should be in archive now
        n = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()
                .count()

        assertEquals(1, n)

        val size = archive.sampleIndividual().individual.size()
        assertEquals(1, size)
    }


    @Test
    fun testFillBuffer_differentSize(){

        config.archiveTargetLimit = 2

        val a = OneMaxIndividual(2)
        a.setValue(0, 0.25)
        var added = archive.addIfNeeded(ff.calculateCoverage(a)!!)
        assertTrue(added)

        val b = OneMaxIndividual(3)
        b.setValue(0, 0.25)
        added = archive.addIfNeeded(ff.calculateCoverage(b)!!)
        assertTrue(added)

        //when this inserted, "b" should be removed, as the longest
        val c = OneMaxIndividual(1)
        c.setValue(0, 0.25)
        added = archive.addIfNeeded(ff.calculateCoverage(c)!!)
        assertTrue(added)

        val sizes = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()

        assertEquals(2, sizes.size)
        assertTrue(sizes.contains(1))
        assertTrue(sizes.contains(2))
    }

    @Test
    fun testFillBuffer_differentScore(){

        config.archiveTargetLimit = 2

        val a = OneMaxIndividual(1)
        a.setValue(0, 0.25)
        archive.addIfNeeded(ff.calculateCoverage(a)!!)


        val b = OneMaxIndividual(1)
        b.setValue(0, 0.75)
        archive.addIfNeeded(ff.calculateCoverage(b)!!)

        //when this inserted, "a" should be removed, as the worst score
        val c = OneMaxIndividual(1)
        c.setValue(0, 0.5)
        archive.addIfNeeded(ff.calculateCoverage(c)!!)


        val sizes = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.fitness.computeFitnessScore()}
                .distinct()

        assertEquals(2, sizes.size)
        assertTrue(sizes.contains(0.5))
        assertTrue(sizes.contains(0.75))
    }

    @Test
    fun testCoveredTwice_differentSize(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(a)!!)

        //not accepted, as longer
        val b = OneMaxIndividual(3)
        b.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(b)!!)

        var sizes = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()

        assertEquals(1, sizes.size)
        assertEquals(2, sizes.first())

        //shorter, so should replace "a"
        val c = OneMaxIndividual(1)
        c.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(c)!!)

        sizes = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()

        assertEquals(1, sizes.size)
        assertEquals(1, sizes.first())
    }


    @Test
    fun testCoveredTwice_differentFitness(){

        val a = OneMaxIndividual(2)
        a.setValue(0, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(a)!!)

        val b = OneMaxIndividual(2)
        b.setValue(1, 1.0) //covering a different branch
        archive.addIfNeeded(ff.calculateCoverage(b)!!)

        var sizes = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()

        assertEquals(1, sizes.size)
        assertEquals(2, sizes.first())

        //better fitness, but still longer on any of those targets
        val c = OneMaxIndividual(3)
        c.setValue(0, 1.0)
        c.setValue(1, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(c)!!)

        sizes = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.individual.size()}
                .distinct()

        assertEquals(1, sizes.size)
        assertEquals(2, sizes.first())


        //same length, but highest overall fitness
        val d = OneMaxIndividual(2)
        d.setValue(0, 1.0)
        d.setValue(1, 1.0)
        archive.addIfNeeded(ff.calculateCoverage(d)!!)

        var scores = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.fitness.computeFitnessScore()}
                .distinct()
        assertEquals(1, scores.size)
        assertEquals(2.0, scores.first(), 0.001)


        //now, add "a" and "b" back. should fail, as they have worse overall fitness than "d"
        assertFalse(archive.addIfNeeded(ff.calculateCoverage(a)!!))
        assertFalse(archive.addIfNeeded(ff.calculateCoverage(b)!!))

        // "d" should still be the only individual
        scores = (1..50).map { archive.sampleIndividual() }
                .map{ind -> ind.fitness.computeFitnessScore()}
                .distinct()
        assertEquals(1, scores.size)
        assertEquals(2.0, scores.first(), 0.001)
    }
}

