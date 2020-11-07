package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FitnessFunction::class.java)
    }

    /**
     * @return [null] if there were problems in calculating the coverage
     */
    fun calculateCoverage(individual: T, targets: Set<Int> = setOf()) : EvaluatedIndividual<T>?{

        val a = individual.seeActions().filter { a -> a.shouldCountForFitnessEvaluations() }.count()

        if(time.averageOverheadMsBetweenTests.isRecordingTimer()){
            time.averageOverheadMsBetweenTests.addElapsedTime()
        }

        var ei = time.measureTimeMillis(
                { t, ind ->
                    time.reportExecutedIndividualTime(t, a)
                    ind?.executionTimeMs = t
                },
                {doCalculateCoverage(individual, targets)}
        )

        processMonitor.eval = ei

        if(ei == null){
            /*
                try again, once. Working with TCP connections and remote servers,
                it is not impossible that sometimes things fail
             */
            log.warn("Failed to evaluate individual. Restarting the SUT before trying again")
            reinitialize()

            //let's wait a little, just in case...
            Thread.sleep(5_000)

            ei = time.measureTimeMillis(
                    {t, ind ->
                        time.reportExecutedIndividualTime(t, a)
                        ind?.executionTimeMs = t
                    },
                    {doCalculateCoverage(individual, targets)}
            )

            if(ei == null){
                //give up, but record it
                log.warn("Failed twice in a row to evaluate individual. Giving up on it.")
                statistics.reportCoverageFailure()
            }
        }

        time.averageOverheadMsBetweenTests.doStartTimer()

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
     * @param targets indicates prioritized targets if there exists
     */
    open fun targetsToEvaluate(targets: Set<Int>, individual: T) : Set<Int>{
        return targets.plus(archive.notCoveredTargets()).filter { !IdMapper.isLocal(it) }.toSet()
    }
}