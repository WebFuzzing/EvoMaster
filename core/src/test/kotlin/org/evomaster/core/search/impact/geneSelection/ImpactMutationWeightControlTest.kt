package org.evomaster.core.search.impact.geneSelection

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.mutationweight.individual.IndividualMutationweightTest
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled

/**
 * created by manzh on 2020-06-03
 */
class ImpactMutationWeightControlTest {

    private lateinit var config: EMConfig
    private lateinit var time : SearchTimeController
    private lateinit var apc: AdaptiveParameterControl
    private lateinit var mwc: MutationWeightControl
    private lateinit var randomness: Randomness

    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(BaseModule()))
                .build().createInjector()
        randomness = injector.getInstance(Randomness::class.java)
        config = injector.getInstance(EMConfig::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
        apc = injector.getInstance(AdaptiveParameterControl::class.java)
        mwc = injector.getInstance(MutationWeightControl::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
        config.maxActionEvaluations = 10
        config.weightBasedMutationRate = true
    }

    @Disabled("TODO ")
    @Test
    fun testIndividual(){

        config.probOfArchiveMutation = 1.0 // disable adaptive mutation rate
        config.d = 0.0 //only based on weight
        time.newActionEvaluation(5)

        val individual = IndividualMutationweightTest.newRestIndividual()
        val all = individual.seeGenes().filter { it.isMutable() }
        val obj = individual.seeGenes(Individual.GeneFilter.NO_SQL).filter { it.isMutable() }.find { it is ObjectGene }
        assertEquals(4, all.size)

        TODO()
    }

}