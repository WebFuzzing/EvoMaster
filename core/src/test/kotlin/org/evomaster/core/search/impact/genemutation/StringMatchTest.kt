package org.evomaster.core.search.impact.genemutation

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.impact.stringmatchproblem.StringMatchIndividual
import org.evomaster.core.search.impact.stringmatchproblem.StringMatchModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach

/**
 * created by manzh on 2020-06-16
 */
class StringMatchTest {

    private lateinit var config: EMConfig
    private lateinit var mio: MioAlgorithm<StringMatchIndividual>

    private val budget = 500

    @BeforeEach
    fun init(){
        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(StringMatchModule(), BaseModule()))
                .build().createInjector()

        mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<StringMatchIndividual>>() {}))

        config = injector.getInstance(EMConfig::class.java)
        config.maxActionEvaluations = budget
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        config.focusedSearchActivationTime = 0.0

    }

    @Test
    fun shareInfoWithASM(){
        config.archiveGeneMutation = EMConfig.ArchiveGeneMutation.SPECIFIED
        config.probOfArchiveMutation = 1.0
        config.baseTaintAnalysisProbability = 0.0

        val solution = mio.search()

        assert(solution.individuals.size > 1)
        val info = solution.individuals.first().individual.gene.mutationInfo

        assert(solution.individuals.all { it.individual.gene.mutationInfo == info })

    }

    @Test
    fun testASM(){

        config.archiveGeneMutation = EMConfig.ArchiveGeneMutation.SPECIFIED
        config.probOfArchiveMutation = 1.0

        val solution = mio.search()

        assertEquals(3, solution.overall.coveredTargets())
    }

    @Test
    fun testWithoutASM(){

        val solution = mio.search()

        assert(solution.overall.coveredTargets() < 3)
    }
}