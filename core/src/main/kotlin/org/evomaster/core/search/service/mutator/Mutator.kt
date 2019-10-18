package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.ImpactUtils
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
    private lateinit var archiveMutator : ArchiveMutator

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
    abstract fun genesToMutation(individual : T, evi: EvaluatedIndividual<T>) : List<Gene>

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
        val targets = archive.notCoveredTargets()

        for (i in 0 until upToNTimes) {

            //save ei before its individual is mutated
            val trackedCurrent = current.copy(tracker.getCopyFilterForEvalInd(current))

            if (!time.shouldContinueSearch()) {
                break
            }
            val mutatedGenes = MutatedGeneSpecification()

            structureMutator.addInitializingActions(current, mutatedGenes)

            if(mutatedGenes.addedInitializationGenes.isNotEmpty() && archiveMutator.enableArchiveSelection()){
                current.updateDbActionGenes(current.individual, mutatedGenes.addedInitializationGenes)
            }
            Lazy.assert{DbActionUtils.verifyActions(current.individual.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutatedInd = mutate(current, targets, mutatedGenes)
            mutatedGenes.setMutatedIndividual(mutatedInd)

            Lazy.assert{DbActionUtils.verifyActions(mutatedInd.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutated = ff.calculateCoverage(mutatedInd)
                    ?: continue

            val reachNew = archive.wouldReachNewTarget(mutated)

            /*
                enable further actions for extracting
             */
            update(trackedCurrent, mutated, mutatedGenes)

            val doesImproved = reachNew || !current.fitness.subsumes(
                    mutated.fitness,
                    targets,
                    config.secondaryObjectiveStrategy,
                    config.bloatControlForSecondaryObjective)

            if (doesImproved) {
                val trackedMutated = if(config.enableTrackEvaluatedIndividual)
                    trackedCurrent.next(this, mutated,tracker.getCopyFilterForEvalInd(trackedCurrent), config.maxLengthOfTraces)!!
                else mutated

                if(config.probOfArchiveMutation > 0.0){
                    trackedMutated.updateImpactOfGenes(true, mutatedGenes, targets, config.secondaryObjectiveStrategy, config.bloatControlForSecondaryObjective)
                }
                archive.addIfNeeded(trackedMutated)
                current = trackedMutated
            }else{
                if(config.probOfArchiveMutation > 0.0){
                    trackedCurrent.getUndoTracking()!!.add(mutated)
                    trackedCurrent.updateImpactOfGenes(false, mutatedGenes, targets, config.secondaryObjectiveStrategy, config.bloatControlForSecondaryObjective)
                }
            }

            // gene mutation evaluation
            if (archiveMutator.enableArchiveGeneMutation()){
                /**
                 * if len(mutatedGenes.mutatedGenes) + len(mutatedGenes.mutatedDbGenes) > 1, shall we evaluate this mutation?
                 */
                mutatedGenes.mutatedGenes.forEachIndexed { index, s->
                    val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, s)
                    val actionIndex = if (mutatedGenes.mutatedPosition.isNotEmpty()) mutatedGenes.mutatedPosition[index] else -1
                    val previousValue = (trackedCurrent.findGeneById(id, actionIndex) ?: throw IllegalStateException("cannot find mutated gene with id ($id) in current individual"))
                    val savedGene = (current.findGeneById(id, actionIndex) ?: throw IllegalStateException("cannot find mutated gene with id ($id) in its original individual"))
                    savedGene.archiveMutationUpdate(original = previousValue, mutated = s, doesCurrentBetter = doesImproved, archiveMutator = archiveMutator)
                }

                mutatedGenes.mutatedDbGenes.forEachIndexed { index, s->
                    val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, s)
                    val actionIndex = if (mutatedGenes.mutatedDbActionPosition.isNotEmpty()) mutatedGenes.mutatedDbActionPosition[index] else -1
                    val savedGene = (current.findGeneById(id, actionIndex, isDb = true) ?: throw IllegalStateException("SQLGene: cannot find mutated Sql- gene with id ($id) in current individual"))
                    /*
                    it may happen, i.e., a gene may be added during 'structureMutator.addInitializingActions(current, mutatedGenes)'
                     */
                    val previousValue = trackedCurrent.findGeneById(id, actionIndex, isDb = true)
                    if (previousValue != null)
                        savedGene.archiveMutationUpdate(original = previousValue, mutated = s, doesCurrentBetter = doesImproved, archiveMutator = archiveMutator)
                }
            }
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