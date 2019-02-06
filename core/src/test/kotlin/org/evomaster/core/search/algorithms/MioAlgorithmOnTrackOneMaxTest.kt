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
import org.evomaster.core.search.service.tracer.TrackingService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class MioAlgorithmOnTrackOneMaxTest {

    val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
                    .build().createInjector()

    @Test
    fun testTrackWithAllHistory(){
        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        val config = injector.getInstance(EMConfig::class.java)
        config.enableTrackIndividual = true
        config.enableTrackEvaluatedIndividual = true
        config.trackLength = -1
        config.maxActionEvaluations = 30000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val randomness = injector.getInstance(Randomness::class.java)
        randomness.updateSeed(42)

        val sampler = injector.getInstance(OneMaxSampler::class.java)


        val n = 20
        sampler.n = n

        val solution = mio.search()

        solution.individuals.forEach { s->
            Assertions.assertNotNull(s.getTrack())
            Assertions.assertNotNull(s.individual.getTrack())
            s.individual.getTrack()?.apply {
                assert(size == s.getTrack()?.size)
                forEachIndexed { index, t ->
                    if(index == 0)
                        assert(t.getDescription().contains(OneMaxSampler::class.java.simpleName))
                    else
                        Assertions.assertEquals(StandardMutator::class.java.simpleName, t.getDescription())
            }
            }
        }
    }

//    @Test
//    fun testTrackWithAllHistoryWithoutEvaluated(){
//        val mio = injector.getInstance(Key.get(
//                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))
//
//        val config = injector.getInstance(EMConfig::class.java)
//        config.enableTrackIndividual = true
//        config.enableTrackEvaluatedIndividual = false
//        config.trackLength = -1
//        config.maxActionEvaluations = 30000
//        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
//
//        val randomness = injector.getInstance(Randomness::class.java)
//        randomness.updateSeed(42)
//
//        val sampler = injector.getInstance(OneMaxSampler::class.java)
//
//
//        val n = 20
//        sampler.n = n
//
//        val solution = mio.search()
//
//        solution.individuals.forEach { s->
//            Assertions.assertNull(s.getTrack())
//            Assertions.assertNotNull(s.individual.getTrack())
//            s.individual.getTrack()!!.apply {
//                forEachIndexed { index, t ->
//                    if(index == 0)
//                        assert(t.getDescription().contains(OneMaxSampler::class.java.simpleName))
//                    else
//                        Assertions.assertEquals(StandardMutator::class.java.simpleName, t.getDescription())
//                }
//            }
//        }
//    }
//
//    @Test
//    fun testTrackWithAllHistoryWithoutIndividual(){
//        val mio = injector.getInstance(Key.get(
//                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))
//
//        val config = injector.getInstance(EMConfig::class.java)
//        config.enableTrackIndividual = false
//        config.enableTrackEvaluatedIndividual = true
//        config.trackLength = -1
//        config.maxActionEvaluations = 30000
//        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
//
//        val randomness = injector.getInstance(Randomness::class.java)
//        randomness.updateSeed(42)
//
//        val sampler = injector.getInstance(OneMaxSampler::class.java)
//
//
//        val n = 20
//        sampler.n = n
//
//        val solution = mio.search()
//
//        solution.individuals.forEach {  s->
//            Assertions.assertNotNull(s.getTrack())
//            Assertions.assertNull(s.individual.getTrack())
//            s.getTrack()!!.forEachIndexed{index, t->
//                if(index == 0)
//                    assert(t.getDescription().contains(OneMaxSampler::class.java.simpleName))
//                else
//                    Assertions.assertEquals(StandardMutator::class.java.simpleName, t.getDescription())
//            }
//        }
//    }

    @Test
    fun testTrackWithSpecifiedHistory(){
        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        val config = injector.getInstance(EMConfig::class.java)
        config.enableTrackIndividual = true
        config.enableTrackEvaluatedIndividual = false
        config.trackLength = 10
        config.maxActionEvaluations = 30000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val randomness = injector.getInstance(Randomness::class.java)
        randomness.updateSeed(42)

        val sampler = injector.getInstance(OneMaxSampler::class.java)

        val trackService = injector.getInstance(TrackingService::class.java)
        trackService.init()

        val n = 20
        sampler.n = n

        val solution = mio.search()

        solution.individuals.forEach { s->
            s.individual.getTrack()!!.apply {
                assert(size in 0..10)
            }
        }
    }

    @Test
    fun testTrackWithoutTrack(){
        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        val config = injector.getInstance(EMConfig::class.java)
        config.enableTrackIndividual = false
        config.enableTrackEvaluatedIndividual = false
        config.maxActionEvaluations = 30000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val randomness = injector.getInstance(Randomness::class.java)
        randomness.updateSeed(42)

        val sampler = injector.getInstance(OneMaxSampler::class.java)

        val trackService = injector.getInstance(TrackingService::class.java)
        trackService.init()

        val n = 20
        sampler.n = n

        val solution = mio.search()

        solution.individuals.forEach { s->
            Assertions.assertNull(s.getTrack())
            Assertions.assertNull(s.individual.getTrack())
        }
    }
}