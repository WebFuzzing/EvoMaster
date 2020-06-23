package org.evomaster.core.search.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxFitness
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import kotlin.math.min

/**
 * during mutation, an individual may be mutated multiple times.
 * the tests are to test whether better individual is identified for next mutation
 */
class MutatorWithOneMaxTest {

    private lateinit var archive: Archive<OneMaxIndividual>
    private lateinit var ff : OneMaxFitness
    private lateinit var config: EMConfig

    private lateinit var mutator : StandardMutator<OneMaxIndividual>
    private lateinit var sampler: OneMaxSampler
    private lateinit var time : SearchTimeController
    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(OneMaxModule(), BaseModule())
                .build().createInjector()


        archive = injector.getInstance(Key.get(
                object : TypeLiteral<Archive<OneMaxIndividual>>() {}))
        mutator = injector.getInstance(Key.get(
                object : TypeLiteral<StandardMutator<OneMaxIndividual>>() {}))

        ff =  injector.getInstance(OneMaxFitness::class.java)
        config = injector.getInstance(EMConfig::class.java)

        sampler = injector.getInstance(OneMaxSampler::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
    }

    @Test
    fun testMutatorWith6Targets(){
        config.maxActionEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        mutatorTest(6, 5000, 1)
    }

    @Test
    fun testMutatorWith100Targets(){
        config.maxActionEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        mutatorTest(100, 5000, 1)
    }

    @Test
    fun testMutatorWith200Targets(){
        config.maxActionEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        mutatorTest(200, 5000, 1)
    }

    @Test
    fun testMutatorWith500Targets(){
        config.maxActionEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        mutatorTest(500, 5000, 1)
    }

    @Test
    fun testMutatorWith1000Targets(){
        config.maxActionEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        mutatorTest(1000, 5000, 1)
    }

    private fun mutatorTest(n: Int, maxEvaluation: Int, sample: Int) {
        sampler.n = n

        var current = ff.calculateCoverage(sampler.sample())!!
        archive.addIfNeeded(current)

        var sampleCounter = 1
        while (sampleCounter < sample){
            sampleCounter += 1
            current = ff.calculateCoverage(sampler.sample())!!
            archive.addIfNeeded(current)
        }

        var fpCounter = 0
        var counter = 0

        val targets = archive.notCoveredTargets().toMutableSet()

        while (!isBest(current) && counter < maxEvaluation){
            counter += 1

            val mutatedGeneSpecification = MutatedGeneSpecification()
            val mutated = improve(current, 0.25, setOf(), mutatedGeneSpecification)?:break
            assertEquals(1, mutatedGeneSpecification.mutatedPosition.size)

            val result = mutator.evaluateMutation(mutated, current, targets, archive)
            if (n <= 100)
                assertEquals(EvaluatedMutation.BETTER_THAN, result)
            else
                assertNotEquals(EvaluatedMutation.WORSE_THAN,result)

            current = mutator.saveMutation(
                    current = current,
                    mutated = mutated,
                    archive = archive,
                    evaluatedMutation = result
            )

            val tp = (mutated == current)
            if (n <= 100)
                assert(tp)
            else if (!tp)
                fpCounter += 1

            targets.addAll(archive.notCoveredTargets())
        }

        if (n > 100)
            assert((fpCounter * 1.0)/counter < 0.1)
    }

    private fun isBest(evaluatedIndividual: EvaluatedIndividual<OneMaxIndividual>) = (0 until evaluatedIndividual.individual.n).all { evaluatedIndividual.individual.getValue(it) == 1.0}

    private fun improve(mutated: EvaluatedIndividual<OneMaxIndividual>, degree : Double, targets : Set<Int>, mutatedGeneSpecification: MutatedGeneSpecification) : EvaluatedIndividual<OneMaxIndividual>?{
        val ind = mutated.individual.copy() as OneMaxIndividual
        val index = (0 until ind.n).firstOrNull{ind.getValue(it)  < 1.0}
        index?:return null
        ind.setValue(index, min(1.0, ind.getValue(index) + degree))
        mutatedGeneSpecification.mutatedPosition.add(index)
        return ff.calculateCoverage(ind, targets)
    }
}