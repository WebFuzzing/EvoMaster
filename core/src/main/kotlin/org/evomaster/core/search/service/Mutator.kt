package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.search.*
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController


abstract class Mutator<T> where T : Individual {

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var ff : FitnessFunction<T>

    @Inject
    protected lateinit var time : SearchTimeController

    @Inject
    protected lateinit var apc: AdaptiveParameterControl

    @Inject
    protected lateinit var structureMutator: StructureMutator

    /**
     * @return a mutated copy
     */
    abstract fun mutate(individual: T) : T


    /**
     * @param upToNTimes how many mutations will be applied. can be less if running out of time
     * @param individual which will be mutated
     * @param archive where to save newly mutated individuals (if needed, eg covering new targets)
     */
    fun mutateAndSave(upToNTimes: Int, individual: EvaluatedIndividual<T>, archive: Archive<T>)
        : EvaluatedIndividual<T> {
        var current = individual

        for(i in 0 until upToNTimes){

            if(! time.shouldContinueSearch()){
                break
            }

            val mutated = ff.calculateCoverage(mutate(current.individual))

            if(current.fitness.subsumes(mutated.fitness, false)){
                continue
            }

            archive.addIfNeeded(mutated)

            current = mutated
        }

        return current
    }
}