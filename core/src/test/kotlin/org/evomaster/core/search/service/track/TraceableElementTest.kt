package org.evomaster.core.search.service.track

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.onemax.OneMaxFitness
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.tracer.ArchiveMutationTrackService

import org.junit.jupiter.api.Test

/**
 * created by manzh on 2020-06-21
 */
class TraceableElementTest {

    val injector: Injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()

    private val config: EMConfig = injector.getInstance(EMConfig::class.java)
    private val sampler : OneMaxSampler = injector.getInstance(OneMaxSampler::class.java)
    private val ff : OneMaxFitness = injector.getInstance(OneMaxFitness::class.java)
    private val tracker : ArchiveMutationTrackService = injector.getInstance(ArchiveMutationTrackService::class.java)

    // disable tracking individual and evaluated individual
    @Test
    fun testOneMaxIndividualWithFF(){
        config.enableTrackIndividual = false
        config.enableTrackEvaluatedIndividual = false
        val inds10 = (0 until 10).map { sampler.sample()}
        inds10.all { it.trackOperator == null }
    }

    // enable tracking individual and but disable evaluated individual
    @Test
    fun testOneMaxIndividualWithTF(){
        config.enableTrackIndividual = true
        config.enableTrackEvaluatedIndividual = false

        val list = mutableListOf<OneMaxIndividual>()
        val ind0 = sampler.sample()
        list.add(ind0)

        (1 until 10).forEach { _->
            val ind = sampler.sample()

        }
    }

    // enable traking evaluated individual but disable individual
    @Test
    fun testEvaluatedOneMaxIndividualWithFT(){
        config.enableTrackEvaluatedIndividual = true
        val evalInds10 = (0 until 10).map { ff.calculateCoverage(sampler.sample()) }

    }

}