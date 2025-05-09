package org.evomaster.core.search.service.track

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.tracer.ArchiveMutationTrackService
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MioAlgorithmOnTrackOneMaxTest {


    private lateinit var config: EMConfig
    private lateinit var mio: MioAlgorithm<OneMaxIndividual>
    private lateinit var tracker : ArchiveMutationTrackService

    private fun init(args: Array<String>){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule(args)))
                .build().createInjector()

        mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        config = injector.getInstance(EMConfig::class.java)
        config.minimize = false

        tracker = injector.getInstance(ArchiveMutationTrackService::class.java)

        val randomness = injector.getInstance(Randomness::class.java)
        randomness.updateSeed(42)

        val sampler = injector.getInstance(OneMaxSampler::class.java)
        val n = 20
        sampler.n = n

    }


    @Test
    fun testIndividualWithTrack(){

        val args = arrayOf(
                "--probOfArchiveMutation",
                "0.0",
                "--weightBasedMutationRate",
                "false",
                "--stoppingCriterion",
                "ACTION_EVALUATIONS",
                "--maxEvaluations",
                "10",
                "--maxLengthOfTraces",
                "50",
                "--enableTrackIndividual",
                "true",
                "--enableTrackEvaluatedIndividual",
                "false"
        )
        init(args)
        assertTrue(tracker.exists(TraceableElementCopyFilter.NONE.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.WITH_TRACK.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.DEEP_TRACK.name))

        val solution = mio.search()

        solution.individuals.forEach { s->
            assertNull(s.tracking)
            assertNotNull(s.individual.trackOperator)
            if (s.individual.trackOperator !is Sampler<*>)
                assertNotNull(s.individual.tracking)
            s.individual.tracking?.history?.apply {
                forEachIndexed { index, t ->
                    if(index == 0)
                        assertTrue(t.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
                    else
                        assertTrue(t.trackOperator!!.operatorTag().contains("Mutator"))
                }
            }
        }
    }

    @Test
    fun testEvaluatedIndividualWithTrack(){

        val args = arrayOf(
                "--stoppingCriterion",
                "ACTION_EVALUATIONS",
                "--enableTrackIndividual",
                "false",
                "--enableTrackEvaluatedIndividual",
                "true",
                "--maxEvaluations",
                "10",
                "--maxLengthOfTraces",
                "50"
        )
        init(args)

        assertTrue(tracker.exists(TraceableElementCopyFilter.NONE.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.WITH_TRACK.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.DEEP_TRACK.name))
        assertTrue(tracker.exists(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED))

        val solution = mio.search()

        solution.individuals.forEach {  s->
            assertNull(s.individual.tracking)
            /**
             * [s] might be null when the individual is never mutated
             */
            if(s.tracking == null){
                assertNotNull(s.individual.trackOperator != null)
                assertTrue(s.individual.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
            }
            s.tracking?.history?.forEachIndexed{ index, t->
                assertNotNull(t.trackOperator)
                if(index == 0)
                    assertTrue(t.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
                else
                    assertEquals(StandardMutator::class.java.simpleName, t.trackOperator!!.operatorTag())
            }
        }
    }

    @Test
    fun testEvaluatedIndividualWithSpecifiedLengthOfTrack(){
        val maxLengthOfTraces = 5
        val args = arrayOf(
                "--stoppingCriterion",
                "ACTION_EVALUATIONS",
                "--enableTrackIndividual",
                "false",
                "--enableTrackEvaluatedIndividual",
                "true",
                "--maxLengthOfTraces",
                maxLengthOfTraces.toString()
        )
        init(args)

        assertTrue(tracker.exists(TraceableElementCopyFilter.NONE.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.WITH_TRACK.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.DEEP_TRACK.name))
        assertTrue(tracker.exists(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED))

        val solution = mio.search()

        assertTrue(solution.individuals.count { it.tracking != null } > 0)

        solution.individuals.forEach {  s->

            assertNull(s.individual.tracking)
            /**
             * [s] might be null when the individual is never mutated
             */
            if(s.tracking == null){
                assertNotNull(s.individual.trackOperator != null)
                assertTrue(s.individual.trackOperator!!.operatorTag().contains(OneMaxSampler::class.java.simpleName))
            }

            s.tracking?.history?.forEachIndexed{ index, t->
                assertNotNull(t.trackOperator)
                assertTrue(index < maxLengthOfTraces)
                //the first one might be sampler
                if(index > 0)
                    assertEquals(StandardMutator::class.java.simpleName, t.trackOperator!!.operatorTag())
            }
        }
    }

    @Test
    fun testEvaluatedIndividualWithTrackImpact(){

        val args = arrayOf(
                "--stoppingCriterion",
                "ACTION_EVALUATIONS",
                "--enableTrackIndividual",
                "false",
                "--enableTrackEvaluatedIndividual",
                "true",
                "--doCollectImpact",
                "true",
                "--maxLengthOfTraces",
                "-1"
        )
        init(args)

        assertTrue(tracker.exists(TraceableElementCopyFilter.NONE.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.WITH_TRACK.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.DEEP_TRACK.name))
        assertTrue(tracker.exists(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED))
        assertTrue(tracker.exists(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT))
        assertTrue(tracker.exists(EvaluatedIndividual.WITH_TRACK_WITH_COPY_IMPACT))
        assertTrue(tracker.exists(EvaluatedIndividual.ONLY_WITH_CLONE_IMPACT))
        assertTrue(tracker.exists(EvaluatedIndividual.ONLY_WITH_COPY_IMPACT))

        val solution = mio.search()

        solution.individuals.forEach {  s->
            assertNull(s.individual.tracking)
            assertNotNull(s.anyImpactInfo())
        }
    }

    @Test
    fun testTrackWithoutTrack(){

        val args = arrayOf(
                "--probOfArchiveMutation",
                "0.0",
                "--weightBasedMutationRate",
                "false",
                "--stoppingCriterion",
                "ACTION_EVALUATIONS",
                "--enableTrackIndividual",
                "false",
                "--enableTrackEvaluatedIndividual",
                "false"
        )
        init(args)
        assertTrue(tracker.exists(TraceableElementCopyFilter.NONE.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.WITH_TRACK.name))
        assertTrue(tracker.exists(TraceableElementCopyFilter.DEEP_TRACK.name))

        val solution = mio.search()

        solution.individuals.forEach { s->
            assertNull(s.tracking)
            assertNull(s.individual.tracking)
        }
    }
}
