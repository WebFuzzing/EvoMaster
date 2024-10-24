package org.evomaster.core.search.impact.weightcalculation

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * created by manzh on 2020-06-03
 */
class ImpactMutationWeightControlTest {

    private lateinit var config: EMConfig
    private lateinit var time : SearchTimeController
    private lateinit var apc: AdaptiveParameterControl
    private lateinit var mwc: MutationWeightControl
    private lateinit var randomness: Randomness
    private lateinit var ags : ArchiveImpactSelector

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
        ags = injector.getInstance(ArchiveImpactSelector::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
        config.maxEvaluations = 10
        config.weightBasedMutationRate = true
    }

    @Test
    fun testWeightCalculation(){
        config.adaptiveGeneSelectionMethod = GeneMutationSelectionMethod.APPROACH_IMPACT

        val targets = setOf(1,2,3)
        val impactG1 = Impact(
                id = "G1",
                degree = 0.0,
                timesToManipulate = 2,
                timesOfNoImpacts = 2,
                timesOfNoImpactWithTargets = mutableMapOf(1 to 2.0,2 to 2.0, 3 to 0.0),
                timesOfImpact = mutableMapOf(1 to 0.0,2 to 0.0, 3 to 1.0),
                noImpactFromImpact = mutableMapOf(1 to 2.0,2 to 2.0, 3 to 0.0),
                noImprovement = mutableMapOf(1 to 2.0,2 to 2.0, 3 to 0.0))
        val impactG2 = Impact(
                id = "G2",
                degree = 0.0,
                timesToManipulate = 2,
                timesOfNoImpacts = 1,
                timesOfNoImpactWithTargets = mutableMapOf(1 to 0.0,2 to 0.0, 3 to 1.0),
                timesOfImpact = mutableMapOf(1 to 2.0,2 to 2.0, 3 to 0.0),
                noImpactFromImpact = mutableMapOf(1 to 0.0,2 to 0.0, 3 to 1.0),
                noImprovement = mutableMapOf(1 to 0.0,2 to 0.0, 3 to 1.0))

        ags.impactBasedOnWeights(listOf(impactG1, impactG2), setOf(1,2)).apply {
            assertEquals(2, size)
            assert(this[0] < this[1])
        }

        ags.impactBasedOnWeights(listOf(impactG1, impactG2), setOf(3)).apply {
            assertEquals(2, size)
            assert(this[0] > this[1])
        }

        ags.impactBasedOnWeights(listOf(impactG1, impactG2), setOf(1,3)).apply {
            assertEquals(2, size)
            assert(this[0] == this[1]) //since they are all 100%
        }

        ags.impactBasedOnWeights(listOf(impactG1, impactG2), setOf(1,2, 3)).apply {
            assertEquals(2, size)
            assert(this[0] < this[1])
        }
    }

}