package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.monitor.SearchProcessMonitor


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

    @Inject
    protected lateinit var statistics: Statistics

    @Inject
    protected lateinit var processMonitor: SearchProcessMonitor

    @Inject
    protected lateinit var config: EMConfig

    /**
     * @return [null] if there were problems in calculating the coverage
     */
    fun calculateCoverage(individual: T, targets: Set<Int> = setOf()) : EvaluatedIndividual<T>?{

        val a = individual.seeActions().filter { a -> a.shouldCountForFitnessEvaluations() }.count()

        var ei = time.measureTimeMillis(
                {time.reportExecutedIndividualTime(it, a)},
                {doCalculateCoverage(individual, targets)}
        )
        processMonitor.eval = ei

        if(ei == null){
            /*
                try again, once. Working with TCP connections and remote servers,
                it is not impossible that sometimes things fail
             */
            reinitialize()
            ei = time.measureTimeMillis(
                    {time.reportExecutedIndividualTime(it, a)},
                    {doCalculateCoverage(individual, targets)}
            )

            if(ei == null){
                //give up, but record it
                statistics.reportCoverageFailure()
            }
        }



        time.newActionEvaluation(maxOf(1, a))
        time.newIndividualEvaluation()

        return ei
    }



    /**
     * calculated coverage with specified targets
     *
     * @return [null] if there were problems in calculating the coverage
     */
    protected abstract fun doCalculateCoverage(individual: T, targets: Set<Int>) : EvaluatedIndividual<T>?

    /**
     * Try to reinitialize the SUT. This is done when there are issues
     * in calculating coverage
     */
    protected open fun reinitialize() = false

    /**
     * decide what targets to evaluate during fitness evaluation
     */
    open fun targetsToEvaluate(targets: Set<Int>, individual: T) : Set<Int>{
        if (targets.isEmpty()) throw IllegalArgumentException("none of the targets to evaluate")
        return targets
    }
}