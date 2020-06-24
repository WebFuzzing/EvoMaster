package org.evomaster.core.search.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.onemax.*
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * during mutation, an individual may be mutated multiple times.
 * the tests are to test whether better individual is identified for next mutation
 */
class MutatorWithOneMaxTest {

    private lateinit var archive: Archive<OneMaxIndividual>
    private lateinit var config: EMConfig

    private lateinit var mutator : ManipulatedOneMaxMutator
    private lateinit var sampler: OneMaxSampler


    private lateinit var mio : MioAlgorithm<OneMaxIndividual>
    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(ManipulatedOneMaxModule(), BaseModule())
                .build().createInjector()


        archive = injector.getInstance(Key.get(
                object : TypeLiteral<Archive<OneMaxIndividual>>() {}))
        mutator = injector.getInstance(Key.get(
                object : TypeLiteral<ManipulatedOneMaxMutator>() {}))

        mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        config = injector.getInstance(EMConfig::class.java)

        sampler = injector.getInstance(OneMaxSampler::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

    }



    private fun reportFP(path : String, improve : Boolean) : Double{
        val contents = Files.readAllLines(Paths.get(config.saveMutatedGeneFile))

        val tp = contents.count {
            val result = it.split(",")[1]
            if (improve) result.equals(EvaluatedMutation.WORSE_THAN.name) else !result.equals(EvaluatedMutation.WORSE_THAN.name)
        }

        return tp * 1.0 / contents.size
    }

    private fun setting(n : Int, improve: Boolean, first : Boolean, budget : Int){
        mutator.improve = improve
        sampler.n = n
        config.mutationTargetsSelectionStrategy = if (first) EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET else EMConfig.MutationTargetsSelectionStrategy.REALTIME_NOT_COVERED_TARGET

        config.maxActionEvaluations = budget

        config.saveMutatedGeneFile = "target/MutatorWithOneMaxTest/targets${n}And${improve}ImproveMutationAnd${first}First.csv"
        Files.deleteIfExists(Paths.get(config.saveMutatedGeneFile))
    }

    @Test
    fun testWith100FT(){
        val n = 100
        val improve = false
        setting(n, improve, true, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith100TT(){
        val n = 100
        val improve = true
        setting(n, improve, true, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith100FF(){
        val n = 100
        val improve = false
        setting(n, improve, false, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith100TF(){
        val n = 100
        val improve = true
        setting(n, improve, false, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith1000FT(){
        val n = 1000
        val improve = false
        setting(n, improve, true, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith1000TT(){
        val n = 1000
        val improve = true
        setting(n, improve, true, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith1000FF(){
        val n = 1000
        val improve = false
        setting(n, improve, false, 100)

        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

    @Test
    fun testWith1000TF(){
        val n = 1000
        val improve = true
        setting(n, improve, false, 100)


        mio.search()

        val result = reportFP(config.saveMutatedGeneFile, improve)

        println(result)

    }

}