package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import kotlin.system.measureTimeMillis


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
    fun calculateCoverage(individual: T) : EvaluatedIndividual<T>?{

        val a = individual.seeActions().filter { a -> a.shouldCountForFitnessEvaluations() }.count()

        var ei = time.measureTimeMillis(
                {time.reportExecutedIndividualTime(it, a)},
                {doCalculateCoverage(individual)}
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
                    {doCalculateCoverage(individual)}
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
     * @return [null] if there were problems in calculating the coverage
     */
    protected abstract fun doCalculateCoverage(individual: T) : EvaluatedIndividual<T>?

    /**
     * Try to reinitialize the SUT. This is done when there are issues
     * in calculating coverage
     */
    protected open fun reinitialize() = false
}