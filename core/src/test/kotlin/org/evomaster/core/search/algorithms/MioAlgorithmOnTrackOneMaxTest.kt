package org.evomaster.core.search.algorithms

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MioAlgorithmOnTrackOneMaxTest {


    private lateinit var config: EMConfig
    private lateinit var mio: MioAlgorithm<OneMaxIndividual>


    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
                .build().createInjector()

        mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        config = injector.getInstance(EMConfig::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS


        val randomness = injector.getInstance(Randomness::class.java)
        randomness.updateSeed(42)

        val sampler = injector.getInstance(OneMaxSampler::class.java)
        val n = 20
        sampler.n = n

    }


    @Test
    fun testIndividualWithTrack(){

        config.enableTrackIndividual = true
        config.enableTrackEvaluatedIndividual = false

        val solution = mio.search()

        solution.individuals.forEach { s->
            assertNull(s.getTracking())
            assertNotNull(s.individual.getTracking())
            s.individual.getTracking()?.apply {
                forEachIndexed { index, t ->
                    if(index == 0)
                        assert(t.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
                    else
                        assert(t.trackOperator!!.operatorTag().contains("Mutator"))
                }
            }
        }
    }

    @Test
    fun testEvaluatedIndividualWithTrack(){

        config.enableTrackIndividual = false
        config.enableTrackEvaluatedIndividual = true

        val solution = mio.search()

        solution.individuals.forEach {  s->
            assertNull(s.individual.getTracking())
            /**
             * [s] might be null when the individual is never mutated
             */
            if(s.getTracking() == null){
                assertNotNull(s.individual.trackOperator != null)
                assert(s.individual.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
            }
            s.getTracking()?.forEachIndexed{ index, t->
                assertNotNull(t.trackOperator)
                if(index == 0)
                    assert(t.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
                else
                    assertEquals(StandardMutator::class.java.simpleName, t.trackOperator!!.operatorTag())
            }
        }
    }


    @Test
    fun testTrackWithoutTrack(){

        config.enableTrackIndividual = false
        config.enableTrackEvaluatedIndividual = false

        val solution = mio.search()

        solution.individuals.forEach { s->
            assertNull(s.getTracking())
            assertNull(s.individual.getTracking())
        }
    }
}
