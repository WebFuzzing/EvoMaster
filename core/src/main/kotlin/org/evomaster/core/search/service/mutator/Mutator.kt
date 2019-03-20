package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.*
import org.evomaster.core.Lazy
import org.evomaster.core.search.gene.Gene
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

    /**
     * @return a mutated next
     */
    abstract fun mutate(individual: EvaluatedIndividual<T>, mutatedGene: MutableList<Gene>): T

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

    /**
     * @param upToNTimes how many mutations will be applied. can be less if running out of time
     * @param individual which will be mutated
     * @param archive where to save newly mutated individuals (if needed, eg covering new targets)
     */
    fun mutateAndSave(upToNTimes: Int, individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T> {

        var curEval = individual
        var targets = archive.notCoveredTargets()

        for (i in 0 until upToNTimes) {

            //save ei before its individual is mutated
            var evalBeforeMutated = curEval.copy(config.enableTrackEvaluatedIndividual)

            if (!time.shouldContinueSearch()) {
                break
            }

            structureMutator.addInitializingActions(curEval)

            Lazy.assert{DbActionUtils.verifyActions(curEval.individual.seeInitializingActions().filterIsInstance<DbAction>())}


            /*
               what genes are mutated within "mutate" function
             */
            val mutatedGenes = mutableListOf<Gene>()
            val mutatedInd = mutate(curEval, mutatedGenes)
//            val mutatedGenesId = mutatedInd.seeGenesIdMap().let {map->
//                        mutatedGenes.map { map.getValue(it) }.toMutableList()
//                    }

            Lazy.assert{DbActionUtils.verifyActions(mutatedInd.seeInitializingActions().filterIsInstance<DbAction>())}

            val mutated = ff.calculateCoverage(mutatedInd)
                    ?: continue

            val reachNew = archive.wouldReachNewTarget(mutated)

            if (reachNew || !curEval.fitness.subsumes(mutated.fitness, targets)) {

                val mutatedWithTracks =
                        if(config.enableTrackEvaluatedIndividual && evalBeforeMutated.isCapableOfTracking()){
                            evalBeforeMutated.next(getTrackOperator(), mutated)!!
                        }else mutated

                if(config.probOfArchiveMutation > 0.0)
                    mutatedWithTracks.updateImpactOfGenes(true)
                val added = archive.addIfNeeded(mutatedWithTracks)
                if(!added){
                    //TODO check if the individual is not added into archive
                }
                curEval = mutatedWithTracks

            }else{
                if(config.probOfArchiveMutation > 0.0){
                    evalBeforeMutated.undoTrack!!.add(mutated)
                    evalBeforeMutated.updateImpactOfGenes(false)
                }
            }
        }

        return curEval
    }

    fun mutateAndSave(individual: EvaluatedIndividual<T>, archive: Archive<T>)
            : EvaluatedIndividual<T>? {

        structureMutator.addInitializingActions(individual)

        return ff.calculateCoverage(mutate(individual, mutableListOf()))
                ?.also { archive.addIfNeeded(it) }
    }
}