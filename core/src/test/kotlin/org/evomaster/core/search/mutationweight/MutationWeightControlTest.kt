package org.evomaster.core.search.mutationweight

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.mutationweight.individual.IndividualMutationweightTest
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * created by manzh on 2020-06-03
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
                .withModules(* arrayOf<Module>(BaseModule(emptyArray(), true)))
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
    fun testGeneSelectionForIndividualBeforeFS(){
        config.weightBasedMutationRate = true
        config.probOfArchiveMutation = 0.0 // disable adaptive mutation rate
        config.d = 0.0 //only based on weight
        config.startingPerOfGenesToMutate = 0.5

        val individual = GeneWeightTestSchema.newRestIndividual(numSQLAction = 0, numRestAction = 8)
        val all = individual.seeGenes().filter { it.isMutable() }
        assertEquals(8, all.size)

        /*
            with 8 genes, avg. of mutated gene = 8 * 0.5
            it is likely to select more than 2 genes to mutate.
         */
        val selected = mutableListOf<Gene>()
        selected.addAll(mwc.selectSubGene(
                candidateGenesToMutate = all,
                adaptiveWeight = false,
                forceNotEmpty = true
        ))

        assert(selected.size >= 2)
    }

    @Test
    fun testGeneSelectionForIndividualWhenFS(){
        config.weightBasedMutationRate = true
        config.probOfArchiveMutation = 0.0 // disable adaptive mutation rate
        config.d = 0.0 //only based on weight
        time.newActionEvaluation(5)

        val individual = GeneWeightTestSchema.newRestIndividual()
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
    fun testGeneSelectionForObjectGeneWhenFS(){
        time.newActionEvaluation(5)

        val individual = GeneWeightTestSchema.newRestIndividual("POST:/gw/efoo")
        val obj = individual.seeGenes(Individual.GeneFilter.NO_SQL).find { it is ObjectGene }

        assertNotNull(obj)

        config.weightBasedMutationRate = true
        config.d = 0.0
        /*
            7 fields, and their weights are 2, 2, 2, 2, 2, 2, 61
            then, m of 7th field are not less than 61/73 when d = 0.0.
            thus when executing 2 times, we assume that 7rd field is selected at least one time
         */
        val resultHW = selectField(obj as ObjectGene, 6, 2, SubsetGeneSelectionStrategy.DETERMINISTIC_WEIGHT)
        assert(resultHW)

        config.weightBasedMutationRate = false

        val einfoObj = (obj.fields.find { it.name == "einfo" } as? OptionalGene)?.gene as? ObjectGene
        assertNotNull(einfoObj)
        /*
              30 fields for einfoObj, it is unlikely to select a specific field at the first attempt, eg, 5th field
         */
        val result = selectField(einfoObj!!, 5, 1, SubsetGeneSelectionStrategy.DEFAULT)
        assertFalse(result)
    }

    private fun selectField(obj: ObjectGene, indexOfField : Int, times : Int, selectionStrategy: SubsetGeneSelectionStrategy) : Boolean{
        val mutatedWH = obj.copy()
        mutatedWH.standardMutation(
                randomness, apc = apc, mwc = mwc, allGenes = listOf(), internalGeneSelectionStrategy = selectionStrategy
        )

        var result = false

        (0..times).forEach { _ ->
            val mf = (mutatedWH as ObjectGene).fields.zip( obj.fields ){ t, o ->
                t.containsSameValueAs(o)
            }
            result = result || !mf[indexOfField]
        }

        return result
    }

}