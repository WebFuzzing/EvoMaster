package org.evomaster.core.search.service

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AdaptiveParameterControlTest{

    private lateinit var config: EMConfig
    private lateinit var time : SearchTimeController
    private lateinit var apc: AdaptiveParameterControl


    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(BaseModule()))
                .build().createInjector()


        config = injector.getInstance(EMConfig::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
        apc = injector.getInstance(AdaptiveParameterControl::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
    }

    @Test
    fun testStart(){

        assertEquals(0.0, time.percentageUsedBudget(), 0.001)

        val n = apc.getExploratoryValue(1, 12)
        assertEquals(1, n)
    }

    @Test
    fun testEnd(){

        config.maxActionEvaluations = 10
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        fakeEvaluation(5) //kicks in focused search
        assertEquals(0.5, time.percentageUsedBudget(), 0.001)

        val max = 12
        assertEquals(max, apc.getExploratoryValue(1, max))

        fakeEvaluation(1)
        assertEquals(0.6, time.percentageUsedBudget(), 0.001)
        assertEquals(max, apc.getExploratoryValue(1, max))

        fakeEvaluation(3)
        assertEquals(0.9, time.percentageUsedBudget(), 0.001)
        assertEquals(max, apc.getExploratoryValue(1, max))
    }

    @Test
    fun testDuring(){
        config.maxActionEvaluations = 10
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val start = 30
        val end = 12
        val k0 = apc.getExploratoryValue(start, end)
        assertEquals(start, k0)

        fakeEvaluation(1)
        val k1 = apc.getExploratoryValue(start, end)
        assertTrue(k0 >= k1)

        fakeEvaluation(1)
        val k2 = apc.getExploratoryValue(start, end)
        assertTrue(k1 >= k2)

        fakeEvaluation(3)
        val k3 = apc.getExploratoryValue(start, end)
        assertTrue(k2 >= k3)
        assertEquals(end, k3)
    }


    private fun fakeEvaluation(n: Int){
        time.newActionEvaluation(n)
    }

}