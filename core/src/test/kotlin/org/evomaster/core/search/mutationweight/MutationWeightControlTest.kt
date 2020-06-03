package org.evomaster.core.search.mutationweight

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

/**
 * created by manzh on 2020-06-02
 */
class MutationWeightControlTest {

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
    }

    @Test
    fun testIndividual(){
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

    @Test
    fun testObjectGene(){
        config.d = 0.0

        val individual = IndividualMutationweightTest.newRestIndividual()
        val obj = individual.seeGenes(Individual.GeneFilter.NO_SQL).find { it is ObjectGene }

        assertNotNull(obj)

        val mutated = obj!!.copy()
        mutated.standardMutation(
                randomness, apc = apc, mwc = mwc, allGenes = listOf(), internalGeneSelectionStrategy = SubsetGeneSelectionStrategy.DETERMINISTIC_WEIGHT
        )

        var result = false
        /*
            3 fields, and their weights are 2, 2, 5
            then, m of 3rd field are not less than 5/9 when d = 1.0.
            thus when executing 2 times, we assume that 3rd field is selected at least one time
         */
        (0..1).forEach { _ ->
            val mf = (mutated as ObjectGene).fields.zip( (obj as ObjectGene).fields ){ t, o ->
                t.containsSameValueAs(o)
            }
            result = result || !mf[2]
        }

        assert(result)
    }


}