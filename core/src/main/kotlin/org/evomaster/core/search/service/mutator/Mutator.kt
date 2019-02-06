package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.*
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.search.service.tracer.TrackOperator

abstract class Mutator<T> : TrackOperator where T : Individual {

    @Inject
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var ff: FitnessFunction<T>

    @Inject
    protected lateinit var time: SearchTimeController

    @Inject
    protected lateinit var apc: AdaptiveParameterControl

    @Inject
    protected lateinit var structureMutator: StructureMutator

    @Inject
    protected lateinit var config: EMConfig

    @Inject
    protected lateinit var rm : ResourceManageService
    /**
     * @return a mutated next
     */
    abstract fun mutate(individual: T): T


    /**
     * @param upToNTimes how many mutations will be applied. can be less if running out of time
     * @param individual which will be mutated
     * @param archive where to save newly mutated individuals (if needed, eg covering new targets)
     */
    fun mutateAndSave(upToNTimes: Int, individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T> {

        var current = individual
        var targets = archive.notCoveredTargets()

        for (i in 0 until upToNTimes) {

            //save ei before its individual is mutated
            var trackedCurrent = current.copy(config.enableTrackEvaluatedIndividual)

            if (!time.shouldContinueSearch()) {
                break
            }

            structureMutator.addInitializingActions(current)

            Lazy.assert{DbActionUtils.verifyActions(current.individual.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutatedInd = mutate(current.individual)

            Lazy.assert{DbActionUtils.verifyActions(mutatedInd.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutated = ff.calculateCoverage(mutatedInd)
                    ?: continue

            val trackedMutated = if(config.enableTrackEvaluatedIndividual && trackedCurrent.isCapableOfTracking()) trackedCurrent.next(getTrackOperator(), mutated)!! else mutated
            val reachNew = archive.wouldReachNewTarget(trackedMutated)

            if (reachNew || !current.fitness.subsumes(trackedMutated.fitness, targets)) {
                if(trackedMutated.individual is RestIndividualII)rm.updateRelevants(trackedMutated.individual, true)
                archive.addIfNeeded(trackedMutated)
                current = trackedMutated
            }else
                if(trackedMutated.individual is RestIndividualII)rm.updateRelevants(trackedMutated.individual, false)
        }

        return current
    }

    fun mutateAndSave(individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T>? {

        structureMutator.addInitializingActions(individual)

        return ff.calculateCoverage(mutate(individual.individual))
                ?.also { archive.addIfNeeded(it) }
    }
}