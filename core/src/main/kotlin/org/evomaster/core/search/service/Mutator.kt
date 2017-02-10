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

    /**
     * @return a mutated copy
     */
    abstract fun mutate(individual: T) : T


    fun mutateAndSave(upToNTimes: Int, individual: EvaluatedIndividual<T>, archive: Archive<T>) : Int{

        var current = individual
        var counter = 0

        for(i in 0 until upToNTimes){
            counter++

            if(! time.shouldContinueSearch()){
                break
            }

            val mutated = ff.calculateCoverage(mutate(current.individual))
            time.newEvaluation()

            if(current.fitness.subsumes(mutated.fitness)){
                continue
            }

            archive.addIfNeeded(mutated)

            current = mutated
        }

        return counter++
    }
}