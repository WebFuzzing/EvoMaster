package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
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
     * decide what targets for coverage calculation
     *
     * @param archive contains info of targets
     * @param mutatedGenes mutated genes info in this mutation
     * @param evi evaluated individual. 'this' mutation is built on the individual of [evi]
     *
     * there may exist many targets, and all of them cannot be evaluated at one time,
     *
     * in the context of impact analysis, instead of e.g., randomly selected 100 not covered targets, we prefer to
     * select those (not covered) which have been impacted by this individual during its evolution
     *
     * TODO we need to find a place (e.g., EMConfig) where to set maximum number (e.g., 100 for REST problem) of targets to evaluate
     * talk to andrea about this.
     */
    private fun getTargetForCoverageCalculation(archive: Archive<T>, mutatedGenes: MutatedGeneSpecification, evi: EvaluatedIndividual<T>) : Set<Int> {

        if (!config.enablePrioritizeTargetsByImpact || config.geneSelectionMethod == GeneMutationSelectionMethod.NONE || mutatedGenes.geneSelectionStrategy == GeneMutationSelectionMethod.NONE) return setOf()

        val all = archive.notCoveredTargets().filter { !IdMapper.isLocal(it) }.toSet()

        if (all.size <= 100) return all
        /*
            if the targets to be evaluated is more than 100,
            we would like to prioritize targets regarding impact info, i.e.,
            1) p1 - prioritize impactful targets regarding mutated genes with 1.0, but no more than 50
            2) p2 - prioritize impactful targets regarding entire individual with 0.8
            3) p3 - avoid targets which are not related to mutated genes with 0.9
            4) p4 - avoid targets which are not related to the individual with 0.7
         */

        val impacts = evi.getImpactsRelatedTo(mutatedGenes)

        val p1 = impacts.flatMap { p->p.shared.timesOfImpact.keys }
        val p2 = setOf<Int>()//evi.getRelatedNotCoveredTarget().run { if (size < 50) this else this.filter { randomness.nextBoolean(0.8) } }
        val p3 = impacts.flatMap { p->p.shared.timesOfNoImpactWithTargets.keys }.filter { randomness.nextBoolean(0.9) }
        val p4 = setOf<Int>()//evi.getNotRelatedNotCoveredTarget().filter { randomness.nextBoolean(0.7) }

        val part1 = all.filter { p1.contains(it) || p2.contains(it) }.run {
            if (size > 80) randomness.choose(this, 80)
            else this
        }
        val selected = part1.plus(all.filter { !part1.contains(it)  && !p3.contains(it) && !p4.contains(it)}.run {
            if (this.isNotEmpty()) randomness.choose(this, 100 - part1.size)
            else this
        })

        if (selected.size < all.size && selected.size < 100)
            return selected.plus(randomness.choose(all.filterNot { selected.contains(it) }, 100 - selected.size)).toSet()
        return selected.toSet()
    }

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
            val trackedCurrent = current.copy(tracker.getCopyFilterForEvalInd(current, deepCopyForImpacts = false))

            if (!time.shouldContinueSearch()) {
                break
            }
            val mutatedGenes = MutatedGeneSpecification()

            structureMutator.addInitializingActions(current, mutatedGenes)

            if(mutatedGenes.addedInitializationGenes.isNotEmpty() && archiveMutator.enableArchiveSelection()){
                current.updateGeneDueToAddedInitializationGenes(
                        genes = mutatedGenes.addedInitializationGenes
                )
            }
            Lazy.assert{DbActionUtils.verifyActions(current.individual.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutatedInd = mutate(current, targets, mutatedGenes)
            mutatedGenes.setMutatedIndividual(mutatedInd)

            Lazy.assert{DbActionUtils.verifyActions(mutatedInd.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutated = ff.calculateCoverage(mutatedInd, getTargetForCoverageCalculation(archive, mutatedGenes, current))
                    ?: continue

            val reachNew = archive.wouldReachNewTarget(mutated)

            /*
                enable further actions for extracting
             */
            update(trackedCurrent, mutated, mutatedGenes)

            val notWorse = reachNew || !current.fitness.subsumes(
                    mutated.fitness,
                    targets,
                    config.secondaryObjectiveStrategy,
                    config.bloatControlForSecondaryObjective)


            val improvedTarget = mutableSetOf<Int>()
            val impactTarget = mutableSetOf<Int>()
            val newTarget = mutableSetOf<Int>()

            archive.wouldReachNewTarget(mutated, newTarget)
            impactTarget.addAll(newTarget)
            improvedTarget.addAll(newTarget)

            if (archiveMutator.enableArchiveMutation()){
                if (improvedTarget.isNotEmpty())
                    impactTarget.addAll(improvedTarget.toSet())

                mutated.fitness.isDifferent(
                        current.fitness,
                        targetSubset = targets,
                        improved = improvedTarget,
                        different = impactTarget,
                        withExtra = false,
                        strategy = config.secondaryObjectiveStrategy,
                        bloatControlForSecondaryObjective = config.bloatControlForSecondaryObjective
                )
            }

            var inArchive = notWorse
            if (notWorse) {
                val trackedMutated = if(config.enableTrackEvaluatedIndividual){
                    trackedCurrent.next(this, mutated,tracker.getCopyFilterForEvalInd(trackedCurrent), config.maxLengthOfTraces)!!
                }
                else mutated

                //if newly initialization actions were added (i.e., trackedCurrent does not include these), impacts should be updated accordingly
                if(mutatedGenes.addedInitializationGenes.isNotEmpty() && archiveMutator.enableArchiveSelection()){
                    trackedMutated.updateGeneDueToAddedInitializationGenes(current)
                }

                if(archiveMutator.enableArchiveSelection()){
                    trackedMutated.updateImpactOfGenes(true,
                            impactTargets = impactTarget, improvedTargets = improvedTarget, mutatedGenes = mutatedGenes)
                }
                inArchive = archive.addIfNeeded(trackedMutated)
                current = trackedMutated
            }

            if (!inArchive && archiveMutator.enableArchiveSelection()){
                current.updateUndoTracking(mutated, config.maxLengthOfTraces)
                current.updateImpactOfGenes(false, impactTargets = impactTarget, improvedTargets = improvedTarget, mutatedGenes = mutatedGenes)
            }

            // gene mutation evaluation
            if (archiveMutator.enableArchiveGeneMutation()){
                /*
                 if len(mutatedGenes.mutatedGenes) + len(mutatedGenes.mutatedDbGenes) > 1, shall we evaluate this mutation?
                 */
                mutatedGenes.mutatedGenes.forEachIndexed { index, s->
                    val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, s)
                    val actionIndex = if (mutatedGenes.mutatedPosition.isNotEmpty()) mutatedGenes.mutatedPosition[index] else -1
                    val previousValue = (trackedCurrent.findGeneById(id, actionIndex) ?: throw IllegalStateException("cannot find mutated gene with id ($id) in current individual"))
                    val savedGene = (current.findGeneById(id, actionIndex) ?: throw IllegalStateException("cannot find mutated gene with id ($id) in its original individual"))
                    savedGene.archiveMutationUpdate(original = previousValue, mutated = s, doesCurrentBetter = notWorse, archiveMutator = archiveMutator)
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
                        savedGene.archiveMutationUpdate(original = previousValue, mutated = s, doesCurrentBetter = notWorse, archiveMutator = archiveMutator)
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