package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Reduce/simplify the final test outputs
 */
class Minimizer<T: Individual> {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(Minimizer::class.java)
    }

    @Inject
    private lateinit var archive: Archive<T>

    @Inject
    private lateinit var fitness : FitnessFunction<T>

    /**
     * Based on the tests in the archive, update the archive by having, for each target T,
     * the minimum number of actions needed to cover it.
     *
     * Using the same/similar kind of algorithm as explained in:
     *
     * "EvoSuite: On The Challenges of Test Case Generation in the Real World"
     */
    fun minimizeActionsPerCoveredTargetInArchive(){

        val current = archive.getCopyOfUniqueCoveringIndividuals()

        LoggingUtil.getInfoLogger().info("Starting to apply minimization phase on ${current.size} tests")

        val beforeCovered = archive.numberOfCoveredTargets()

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

        TODO



    }
}