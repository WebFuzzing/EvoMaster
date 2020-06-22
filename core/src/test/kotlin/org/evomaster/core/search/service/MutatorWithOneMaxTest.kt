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
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.math.max
import kotlin.math.min

/**
 * created by manzh on 2020-06-18
 */
class MutatorWithOneMaxTest {

    private lateinit var archive: Archive<OneMaxIndividual>
    private lateinit var ff : OneMaxFitness
    private lateinit var config: EMConfig
    private lateinit var randomness: Randomness

    private lateinit var mutator : StandardMutator<OneMaxIndividual>

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

        randomness = injector.getInstance(Randomness::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
    }

    // whether identify better individual for next mutation
    @Test
    fun evaluateMutationTest() {
        // 6 targets
        val n = 100
        val first = OneMaxIndividual(n)
        first.setValue(0, 0.25)
        var current = ff.calculateCoverage(first)!!
        archive.addIfNeeded(current)

        val targets = archive.notCoveredTargets().toMutableSet()
        while (!isBest(current)){
            val mutated = improve(current, 0.25)?:break
            val result = mutator.evaluateMutation(mutated, current, targets, archive)
            assertEquals(EvaluatedMutation.BETTER_THAN, result)

            current = mutator.saveMutation(
                    current = current,
                    mutated = mutated,
                    archive = archive,
                    evaluatedMutation = result
            )
            assertEquals(mutated, current,
                    "targets: ${targets.joinToString(",")}, mutated: ${ (0 until n).map { mutated.individual.getValue(it) }.joinToString(",") }")

            targets.addAll(archive.notCoveredTargets())
        }
    }

    private fun isBest(evaluatedIndividual: EvaluatedIndividual<OneMaxIndividual>) = (0 until evaluatedIndividual.individual.n).all { evaluatedIndividual.individual.getValue(it) == 1.0}

    private fun improve(mutated: EvaluatedIndividual<OneMaxIndividual>, degree : Double) : EvaluatedIndividual<OneMaxIndividual>?{
        val ind = mutated.individual.copy() as OneMaxIndividual
        val index = (0 until ind.n).firstOrNull{ind.getValue(it)  < 1.0}
        index?:return null
        ind.setValue(index, min(1.0, ind.getValue(index) + degree))
        return ff.calculateCoverage(ind)
    }
}