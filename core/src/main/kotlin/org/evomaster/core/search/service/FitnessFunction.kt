package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual


abstract class FitnessFunction<T>  where T : Individual {

    @Inject
    protected lateinit var configuration: EMConfig

    @Inject
    protected lateinit var archive: Archive<T>

    @Inject
    protected lateinit var idMapper: IdMapper

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var time: SearchTimeController

    fun calculateCoverage(individual: T) : EvaluatedIndividual<T>{
        val ei = doCalculateCoverage(individual)

        val a = individual.seeActions().filter { a -> a.shouldCountForFitnessEvaluations() }.count()

        time.newActionEvaluation(maxOf(1, a))
        time.newIndividualEvaluation()

        return ei
    }

    protected abstract fun doCalculateCoverage(individual: T) : EvaluatedIndividual<T>

}