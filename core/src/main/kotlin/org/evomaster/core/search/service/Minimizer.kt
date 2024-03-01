package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.gui.GuiIndividual
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.core.sql.SqlAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Reduce/simplify the final test outputs.
 *
 * WARN: currently minimization loses all history info from EvaluatedIndividual, eg impact of genes
 */
class Minimizer<T: Individual> {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(Minimizer::class.java)
    }

    @Inject
    private lateinit var archive: Archive<T>

    @Inject
    private lateinit var fitness : FitnessFunction<T>

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var idMapper: IdMapper

    private var startTimer : Long = -1


    fun doStartTheTimer(){
        startTimer = System.currentTimeMillis()
    }

    fun passedTimeInSecond() : Int {
        if(startTimer < 0){
            throw IllegalStateException("Timer was not started")
        }
        return ((System.currentTimeMillis() - startTimer) / 1000).toInt()
    }

    private fun checkHasTimedout() : Boolean{
        if(startTimer < 0){
            throw IllegalStateException("Timer was not started")
        }
        if(config.minimizeTimeout < 0){
            return false
        }
        if(config.minimizeTimeout == 0){
            return true
        }
        val current = System.currentTimeMillis()
        val passed = (current - startTimer) / (1000 * 60.0)
        return passed > config.minimizeTimeout
    }

    fun pruneNonNeededDatabaseActions(){
        //TODO
    }

    /**
     * eg, removed un-needed optional parameters
     */
    fun simplifyActions(){
        //TODO
    }

    /**
     * Based on the tests in the archive, update the archive by having, for each target T,
     * the minimum number of actions needed to cover it.
     *
     * Using the same/similar kind of algorithm as explained in:
     *
     * "EvoSuite: On The Challenges of Test Case Generation in the Real World"
     */
    fun minimizeMainActionsPerCoveredTargetInArchive() {

        if(checkHasTimedout()){
           LoggingUtil.getInfoLogger().warn("Minimization phase has timed-out. You can use --minimizeTimeout to increase it.")
           return
        }

        LoggingUtil.getInfoLogger().info("Starting to apply minimization phase")

        recomputeArchiveWithFullCoverageInfo()

        val current = archive.getCopyOfUniqueCoveringIndividuals()
            .filter {
                it.size() > 1
            } //can't minimize below 1

        if(current.isEmpty()){
            LoggingUtil.getInfoLogger().info("No test to minimize")
            return
        }

        LoggingUtil.getInfoLogger().info("Analyzing ${current.size} tests with size greater than 1")

        val n = current.size
        var k = 0;

        if(config.showProgress){
            println()
            printProgress(k,n)
        }

        current.forEach{

            if(checkHasTimedout()){
                LoggingUtil.getInfoLogger().warn("Minimization phase has timed-out. You can use --minimizeTimeout to increase it.")
                return
            }

            k++

            if(it !is GuiIndividual) { //doesn't make sense for GUI sequences, as strongly dependent
                val singles = splitIntoSingleCalls(it)
                singles.forEach { s ->
                    fitness.computeWholeAchievedCoverageForPostProcessing(s)?.run { archive.addIfNeeded(this) }
                }
            }

            /*
                Above works well if each action is independent.
                To handle cases in which there is need of more than 1 action together, we need to remove each
                action one at a time, and see if there is any improvement. For example, having size N,
                remove from 0 to N, one at a time. If any of those is successful (ie addIfNeeded returns true), then
                re-apply recursively to all the successful copies with length N-1.

                TODO implement such algorithm
             */

            printProgress(k,n)
        }
    }



    private fun splitIntoSingleCalls(ind: T) : List<T>{

        val n = ind.size()

        if(n <= 1){
            throw IllegalArgumentException("Need at least 2 actions to apply split")
        }

        val copy =  ind.copy()

        return (0 until n)
            .map {index ->  (copy.copy() as T)
                                .apply {
                                    removeAllMainActionsButIndex(this,index)
                                }
            }
    }

    private fun removeAllMainActionsButIndex(ind: T, index: Int){

        val n = ind.size()

        val sqlActions = if (ind is EnterpriseIndividual) ind.seeSQLActionBeforeIndex(index).map { it.copy() as SqlAction} else null

        for(i in n-1 downTo index+1){
            ind.removeMainExecutableAction(i)
        }
        for(i in 0 until index){
            ind.removeMainExecutableAction(0)
        }

        if (!sqlActions.isNullOrEmpty()){
            ind.addChildrenToGroup(sqlActions, GroupsOfChildren.INITIALIZATION_SQL)
        }

        if (!ind.verifyBindingGenes()){
            ind.cleanBrokenBindingReference()
            ind.computeTransitiveBindingGenes()
        }

        if(ind is RestIndividual){
            ind.removeLocationId()
        }
    }

    private fun printProgress(k: Int, n: Int){
        if(! config.showProgress){
            return
        }

        //TODO check if need a time-delta for updates, as in SearchStatusUpdater

        SearchStatusUpdater.upLineAndErase()
        println("* Minimization progress: $k/$n")
    }

    private fun recomputeArchiveWithFullCoverageInfo(){
        val current = archive.getCopyOfUniqueCoveringIndividuals()
            .onEach { if(it is EnterpriseIndividual) it.ensureFlattenedStructure()  }

        LoggingUtil.getInfoLogger().info("Recomputing full coverage for ${current.size} tests")

        val beforeCovered = archive.coveredTargets()

        /*
            Previously evaluated individual only had partial info, due to performance issues.
            Need to make sure to fetch all coverage info.
         */
        val population = current.mapNotNull {
            val ei = fitness.computeWholeAchievedCoverageForPostProcessing(it)
            if(ei == null){
                log.warn("Failed to re-evaluate individual during minimization")
            }
            ei
        }

        archive.clearPopulations()

        population.forEach{archive.addIfNeeded(it)}

        val afterCovered = archive.coveredTargets()
        val diff = beforeCovered.size-afterCovered.size

        if(diff > config.minimizeThresholdForLoss *  beforeCovered.size){
            //could happen if background threads, for example, as well as constructors of singletons
            LoggingUtil.getInfoLogger().warn("Recomputing coverage did lose many targets," +
                    " more than the threshold ${config.minimizeThresholdForLoss*100}%:" +
                    " from ${beforeCovered.size} to ${afterCovered.size}" +
                    ", i.e., lost $diff")

            if(config.minimizeShowLostTargets){
                LoggingUtil.getInfoLogger().warn("Missing targets:");
                for(target in beforeCovered){
                    if(! afterCovered.contains(target)){
                        LoggingUtil.getInfoLogger().warn(idMapper.getDescriptiveId(target));
                    }
                }
            }

            assert(false)//shouldn't really happen in the E2E...
        }
    }
}
