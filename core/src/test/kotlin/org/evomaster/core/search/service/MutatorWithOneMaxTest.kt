package org.evomaster.core.search.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.onemax.*
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Paths

/**
 * during mutation, an individual may be mutated multiple times.
 * the tests are to test whether better individual is identified for next mutation
 */
class MutatorWithOneMaxTest {

    private lateinit var injector: Injector
    private lateinit var config : EMConfig
    private lateinit var manager : LifecycleManager

    @BeforeEach
    fun init(){

        injector = LifecycleInjector.builder()
                .withModules(ManipulatedOneMaxModule(), BaseModule())
                .build().createInjector()

        manager = injector.getInstance(LifecycleManager::class.java)
        config = injector.getInstance(EMConfig::class.java)
    }

    private fun reportFP(path : String, improve : Boolean) : Double{
        val contents = Files.readAllLines(Paths.get(path))

        val tp = contents.count {
            val result = it.split(",")[1]
            if (improve) result.equals(EvaluatedMutation.WORSE_THAN.name) else !result.equals(EvaluatedMutation.WORSE_THAN.name)
        }

        return tp * 1.0 / contents.size
    }

    private fun setting(n : Int, improve: Boolean, first : Boolean, budget : Int) : Double{
        injector.getInstance(ManipulatedOneMaxMutator::class.java).improve = improve
        injector.getInstance(OneMaxSampler::class.java).n = n
        config.mutationTargetsSelectionStrategy = if (first) EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET else EMConfig.MutationTargetsSelectionStrategy.EXPANDED_UPDATED_NOT_COVERED_TARGET

        config.maxActionEvaluations = budget
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        config.saveMutatedGeneFile = "target/MutatorWithOneMaxTest/targets${n}And${improve}ImproveMutationAnd${first}First.csv"
        Files.deleteIfExists(Paths.get(config.saveMutatedGeneFile))

        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        manager.start()
        mio.search()
        manager.close()

        return reportFP(config.saveMutatedGeneFile, improve)
    }

    @Test
    fun testWith100TT(){
        val n = 100
        val improve = true
        val result = setting(n, improve, true, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    @Test
    fun testWith200TT(){
        val n = 200
        val improve = true
        val result = setting(n, improve, true, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    @Test
    fun testWith500TT(){
        val n = 500
        val improve = true
        val result = setting(n, improve, true, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    @Test
    fun testWith100TF(){
        val n = 100
        val improve = true
        val result = setting(n, improve, false, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    @Test
    fun testWith200TF(){
        val n = 200
        val improve = true
        val result = setting(n, improve, false, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    @Test
    fun testWith500TF(){
        val n = 500
        val improve = true
        val result = setting(n, improve, false, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }
}