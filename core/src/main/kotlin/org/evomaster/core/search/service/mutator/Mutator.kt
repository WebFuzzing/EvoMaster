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
    abstract fun mutate(individual: EvaluatedIndividual<T>, mutatedGenes: MutatedGeneSpecification? = null): T

    /**
     * @param individual an individual to mutate
     * @param evi a reference of the individual to mutate
     * @return a list of genes that are allowed to mutate
     */
    abstract fun genesToMutation(individual : T, evi: EvaluatedIndividual<T>) : List<Gene>

    /**
     * @param individual an individual to mutate
     * @param evi a reference of the individual to mutate
     * @return a list of genes that are selected to mutate
     */
    abstract fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>) : List<Gene>

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

            structureMutator.addInitializingActions(current)

            Lazy.assert{DbActionUtils.verifyActions(current.individual.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutatedGenes = MutatedGeneSpecification()
            val mutatedInd = mutate(current, mutatedGenes)
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
                    trackedMutated.updateImpactOfGenes(true, mutatedGenes, targets, config.secondaryObjectiveStrategy)
//                    if (config.archiveGeneMutation)
//                        trackedMutated.mutatedGeneSpecification = mutatedGenes.copyFrom(trackedMutated)
                }
                archive.addIfNeeded(trackedMutated)
                current = trackedMutated
            }else{
                if(config.probOfArchiveMutation > 0.0){
                    trackedCurrent.getUndoTracking()!!.add(mutated)
                    trackedCurrent.updateImpactOfGenes(false, mutatedGenes, targets, config.secondaryObjectiveStrategy)
                }
            }

            /*
            TODO, handle StringGene that are inside of root gene
             */
            // gene mutation evaluation
            if (config.probOfArchiveMutation > 0.0 && config.archiveGeneMutation != EMConfig.ArchiveGeneMutation.NONE){
                mutatedGenes.mutatedGenes.filter { archiveMutator.doesSupport(it) }.forEach { s->
                    val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, s)
                    val savedGene = (current.findGeneById(id) ?: throw IllegalStateException("mismatched genes"))
                    val previousValue = (trackedCurrent.findGeneById(id) ?: throw IllegalStateException("mismatched genes"))
                    savedGene.archiveMutationUpdate(original = previousValue, mutated = s, doesCurrentBetter = doesImproved, archiveMutator = archiveMutator)
                    //(savedGene as StringGene).validateMutationUpdate(archiveMutator)
                }
            }

//            current.individual.seeGenes().filterIsInstance<StringGene>().forEach {
//                it.validateMutationUpdate(archiveMutator)
//            }
        }
        return current
    }

    fun mutateAndSave(individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T>? {

        structureMutator.addInitializingActions(individual)

        return ff.calculateCoverage(mutate(individual))
                ?.also { archive.addIfNeeded(it) }
    }

}