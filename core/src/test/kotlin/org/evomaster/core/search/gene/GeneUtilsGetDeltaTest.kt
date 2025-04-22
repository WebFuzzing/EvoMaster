package org.evomaster.core.search.gene

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.math.pow

class GeneUtilsGetDeltaTest {

    private lateinit var config: EMConfig
    private lateinit var time : SearchTimeController
    private lateinit var apc: AdaptiveParameterControl
    private lateinit var randomness: Randomness

    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(BaseModule(emptyArray(), true)))
            .build().createInjector()


        config = injector.getInstance(EMConfig::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
        apc = injector.getInstance(AdaptiveParameterControl::class.java)
        randomness = injector.getInstance(Randomness::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
    }


    companion object{
        @JvmStatic
        fun getBudgetAndRange(): Stream<Arguments> {
            val budget = arrayOf(10, 100, 1000, 10000, 100000)
            // 2^0 -- 2^30, 10^0 -- 10^15
            val range = (0..30).map { 2.0.pow(it).toLong() }.plus((0..15).map { 10.0.pow(it).toLong() })
            return range.map {r-> budget.map { Arguments.of(it, r) } }.flatten().stream()
        }
    }


    @ParameterizedTest
    @ValueSource(ints = [-1, 0])
    fun testInValidRange(range : Int){
        assertThrows<IllegalArgumentException> { GeneUtils.getDelta(randomness, apc, range = range.toLong()) }
    }

    @ParameterizedTest
    @MethodSource("getBudgetAndRange")
    fun testGetDelta(iteration: Int, range: Long) {
        config.maxEvaluations = iteration
        (0 until iteration).forEach { _ ->
            fakeOneEvaluation()
            val delta = GeneUtils.getDelta(randomness, apc, range = range)
            assertTrue(delta <= range, "delta $delta should not be greater than range $range")
        }
    }


    private fun fakeOneEvaluation(){
        time.newActionEvaluation(1)
    }
}