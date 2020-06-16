package org.evomaster.core.search.impact.geneMutation

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.impact.geneMutation.stringMatch.StringMatchIndividual
import org.evomaster.core.search.impact.geneMutation.stringMatch.StringMatchModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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