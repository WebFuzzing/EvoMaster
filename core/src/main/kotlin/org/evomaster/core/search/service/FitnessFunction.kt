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
     *
     * @param targetIds do calculate coverage regarding [targetIds]
     */
    fun calculateCoverage(individual: T, targetIds: Set<Int> = setOf()) : EvaluatedIndividual<T>?{

        val ids = if (targetIds.isEmpty()) defaultTargetForCoverageCalculation() else targetIds

        var ei = doCalculateCoverage(individual, ids)
        processMonitor.eval = ei

        if(ei == null){
            /*
                try again, once. Working with TCP connections and remote servers,
                it is not impossible that sometimes things fail
             */
            reinitialize()
            ei = doCalculateCoverage(individual, ids)

            if(ei == null){
                //give up, but record it
                statistics.reportCoverageFailure()
            }
        }

        val a = individual.seeActions().filter { a -> a.shouldCountForFitnessEvaluations() }.count()

        time.newActionEvaluation(maxOf(1, a))
        time.newIndividualEvaluation()

        return ei
    }

    /**
     * @return [null] if there were problems in calculating the coverage
     *
     * @param evaluatedIndividual evaluated [individual] if it exists
     * this param can be used to evaluate individual after mutation. At this phase, [evaluatedIndividual] is available that
     * may help to select targets for fitness evaluation with additional info, e.g., impact.
     *
     * */
    protected abstract fun doCalculateCoverage(individual: T, targetIds: Set<Int>) : EvaluatedIndividual<T>?

    /**
     * Try to reinitialize the SUT. This is done when there are issues
     * in calculating coverage
     */
    protected open fun reinitialize() = false


    /**
     * there may exist many targets, and all of them cannot be evaluated at one time,
     *
     * in the context of impact analysis, instead of randomly selected 100 targets, we prefer to
     * select not covered targets which have been impacted by this individual during its evolution
     *
     * @param evaluatedIndividual evaluated individual if exists
     *
     * TODO shall we need targets specific to mutated genes?
     */
    open fun defaultTargetForCoverageCalculation() : Set<Int> = setOf()
}