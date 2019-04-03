package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.*
import org.evomaster.core.Lazy
import org.evomaster.core.search.tracer.TrackOperator

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

    /**
     * @return a mutated copy
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
            var trackedCurrent = if(config.enableTrackEvaluatedIndividual) current.forceCopyWithTrack() else current.copy(config.enableTrackIndividual)

            if (!time.shouldContinueSearch()) {
                break
            }

            structureMutator.addInitializingActions(current)

            Lazy.assert{DbActionUtils.verifyActions(current.individual.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutatedInd = mutate(current.individual)

            Lazy.assert{DbActionUtils.verifyActions(mutatedInd.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutated = ff.calculateCoverage(mutatedInd)
                    ?: continue

            val reachNew = archive.wouldReachNewTarget(mutated)

            if (reachNew || !current.fitness.subsumes(mutated.fitness, targets)) {
                val trackedMutated = if(config.enableTrackEvaluatedIndividual) trackedCurrent.next(this, mutated)!! else mutated
                archive.addIfNeeded(trackedMutated)
                current = trackedMutated
            }
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