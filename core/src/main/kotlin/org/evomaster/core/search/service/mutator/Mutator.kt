package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactInfoCollection.GeneMutationSelectionMethod
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.tracer.ArchiveMutationTrackService
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

    @Inject
    private lateinit var tracker : ArchiveMutationTrackService

    @Inject
    protected lateinit var archiveMutator : ArchiveMutator

    @Inject
    protected lateinit var mwc : MutationWeightControl

    /**
     * @param mutatedGenes is used to record what genes are mutated within [mutate], which can be further used to analyze impacts of genes.
     * @return a mutated copy
     */
    abstract fun mutate(individual: EvaluatedIndividual<T>, targets: Set<Int> = setOf(), mutatedGenes: MutatedGeneSpecification? = null): T

    /**
     * @param individual an individual to mutate
     * @param evi a reference of the individual to mutate
     * @return a list of genes that are allowed to mutate
     */
    abstract fun genesToMutation(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>) : List<Gene>

    /**
     * @param individual an individual to mutate
     * @param evi a reference of the individual to mutate
     * @param targets to cover with this mutation
     * @return a list of genes that are selected to mutate
     */
    abstract fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int> = setOf(), mutatedGenes: MutatedGeneSpecification?) : List<Gene>

    /**
     * @return whether do a structure mutation
     */
    abstract fun doesStructureMutation(individual : T) : Boolean

    open fun postActionAfterMutation(individual: T){}

    open fun update(previous: EvaluatedIndividual<T>, mutated : EvaluatedIndividual<T>, mutatedGenes: MutatedGeneSpecification?){}

    /**
     * @param upToNTimes how many mutations will be applied. can be less if running out of time
     * @param individual which will be mutated
     * @param archive where to save newly mutated individuals (if needed, eg covering new targets)
     */
    fun mutateAndSave(upToNTimes: Int, individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T> {

        var current = individual
        val targets = archive.notCoveredTargets().toMutableSet()

        for (i in 0 until upToNTimes) {

            //save ei before its individual is mutated
            val trackedCurrent = current.copy(tracker.getCopyFilterForEvalInd(current, deepCopyForImpacts = false))

            if (!time.shouldContinueSearch()) {
                break
            }
            val mutatedGenes = MutatedGeneSpecification()

            structureMutator.addInitializingActions(current, mutatedGenes)

            Lazy.assert{DbActionUtils.verifyActions(current.individual.seeInitializingActions().filterIsInstance<DbAction>())}

            // should only use notcovered targets
            val mutatedInd = mutate(individual = current, targets = archive.notCoveredTargets(), mutatedGenes = mutatedGenes)
            mutatedGenes.setMutatedIndividual(mutatedInd)

            Lazy.assert{DbActionUtils.verifyActions(mutatedInd.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutated = ff.calculateCoverage(mutatedInd)
                    ?: continue

            val reachNew = archive.wouldReachNewTarget(mutated)
            archiveMutator.saveMutatedGene(mutatedGenes, mutatedInd, time.evaluatedIndividuals, reachNew)
            archive.saveSnapshot()

            /*
                enable further actions for extracting
             */
            update(trackedCurrent, mutated, mutatedGenes)

            val notWorse = reachNew || !current.fitness.subsumes(
                    mutated.fitness,
                    targets,
                    config.secondaryObjectiveStrategy,
                    config.bloatControlForSecondaryObjective)

            /*
                key is target
                value is impactful info, i.e., 0 -- no any impact, -1 -- become worse, 1 -- become better
             */
            val targetsEvaluated = targets.map { it to 0 }.toMap().toMutableMap()

            archive.wouldReachNewTarget(mutated, targetsEvaluated)

            if (config.probOfArchiveMutation > 0.0){

                mutated.fitness.isDifferent(
                        current.fitness,
                        targetSubset = targets,
                        targetInfo = targetsEvaluated,
                        withExtra = false,
                        strategy = config.secondaryObjectiveStrategy,
                        bloatControlForSecondaryObjective = config.bloatControlForSecondaryObjective
                )
            }

            var inArchive = notWorse
            if (notWorse) {
                val trackedMutated = if(config.enableTrackEvaluatedIndividual){
                    trackedCurrent.next(this, mutated,tracker.getCopyFilterForEvalInd(trackedCurrent), config.maxLengthOfTraces)!!
                } else mutated

                //if newly initialization actions were added (i.e., trackedCurrent does not include these), impacts should be updated accordingly
                if(mutatedGenes.addedInitializationGenes.isNotEmpty() && archiveMutator.enableArchiveSelection()){
                    trackedMutated.updateGeneDueToAddedInitializationGenes(current)
                }

                if(archiveMutator.doCollectImpact()){
                    trackedMutated.updateImpactOfGenes(true,
                            targetsInfo = targetsEvaluated, mutatedGenes = mutatedGenes)
                }
                inArchive = archive.addIfNeeded(trackedMutated)
                current = trackedMutated
            }

            if (!inArchive){
                if (config.enableTrackEvaluatedIndividual)
                    current.updateUndoTracking(mutated, config.maxLengthOfTraces)
                if (archiveMutator.doCollectImpact())
                    current.updateImpactOfGenes(false, mutatedGenes = mutatedGenes, targetsInfo = targetsEvaluated)
            }

            archiveMutator.saveImpactSnapshot(time.evaluatedIndividuals, checkedTargets = targets,targetsInfo = targetsEvaluated, addedToArchive = inArchive, evaluatedIndividual = current)

            // gene mutation evaluation
            if (archiveMutator.enableArchiveGeneMutation()){
                archiveMutator.updateArchiveMutationInfo(trackedCurrent, current, mutatedGenes, targetsEvaluated)
            }

            targets.addAll(archive.notCoveredTargets())
        }
        return current
    }

    fun mutateAndSave(individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T>? {

        structureMutator.addInitializingActions(individual,null)

        return ff.calculateCoverage(mutate(individual))
                ?.also { archive.addIfNeeded(it) }
    }

}