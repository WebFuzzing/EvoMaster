package org.evomaster.core.search.impact.genemutation

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.matchproblem.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach

/**
 * created by manzh on 2020-06-16
 */
class IntegerGeneMatchTest {

    private lateinit var config: EMConfig
    private lateinit var mio: MioAlgorithm<PrimitiveTypeMatchIndividual>

    private val budget = 6000

    @BeforeEach
    fun init(){
        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(PrimitiveTypeMatchModule(), BaseModule()))
                .build().createInjector()

        mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<PrimitiveTypeMatchIndividual>>() {}))

        config = injector.getInstance(EMConfig::class.java)
        config.maxActionEvaluations = budget
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val sampler = injector.getInstance(PrimitiveTypeMatchSampler::class.java)
        sampler.template = PrimitiveTypeMatchIndividual.intTemplate()

        val ff = injector.getInstance(PrimitiveTypeMatchFitness::class.java)
        ff.type = ONE2M.ONE_EQUAL_WITH_ONE

    }

    @Test
    fun testASM(){

        config.weightBasedMutationRate = true
        config.enableTrackEvaluatedIndividual = true
        config.archiveGeneMutation = EMConfig.ArchiveGeneMutation.SPECIFIED
        config.probOfArchiveMutation = 1.0

        val solution = mio.search()

        assertEquals(5, solution.overall.coveredTargets())
    }

    @Test
    fun testWithoutASM(){

        val solution = mio.search()

        assert(solution.overall.coveredTargets() < 5)
    }
}