package org.evomaster.core.search.mutationweight

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import io.swagger.parser.OpenAPIParser
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.service.RestModule
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.mutationweight.individual.IndividualMutationweightTest
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * created by manzh on 2020-06-02
 */
class MutationWeightControlTest {

    private lateinit var config: EMConfig
    private lateinit var time : SearchTimeController
    private lateinit var apc: AdaptiveParameterControl
    private lateinit var mwc: MutationWeightControl

    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(BaseModule()))
                .build().createInjector()

        config = injector.getInstance(EMConfig::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
        apc = injector.getInstance(AdaptiveParameterControl::class.java)
        mwc = injector.getInstance(MutationWeightControl::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
        config.maxActionEvaluations = 10
    }

    @Test
    fun testSelectedGenesBasedWeight(){
        config.weightBasedMutationRate = true
        config.probOfArchiveMutation = 0.0 // disable adaptive mutation rate
        config.d = 0.0 //only based on weight
        config.startingPerOfGenesToMutate = 0.01

        val individual = IndividualMutationweightTest.newRestIndividual()
        val all = individual.seeGenes().filter { it.isMutable() }
        val obj = individual.seeGenes(Individual.GeneFilter.NO_SQL).filter { it.isMutable() }.find { it is ObjectGene }
        assertEquals(4, all.size)

        /*
            weights for the four genes are 1,1,2,9.
            - m for obj is 9/13 = 0.7 when d = 0, t = 1.
            - when we execute the selection 3 time, a probability that obj is never selected is 0.3 * 0.3 * 0.3
         */
        val selected = mutableListOf<Gene>()
        (0..2).forEach { _ ->
            selected.addAll(mwc.selectSubGene(
                    candidateGenesToMutate = all,
                    adaptiveWeight = false,
                    forceNotEmpty = true
            ))
        }
        assert(selected.count { it == obj } >= 1)
    }


}