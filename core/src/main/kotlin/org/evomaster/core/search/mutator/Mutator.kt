package org.evomaster.core.search.mutator

import com.google.inject.Inject
import org.evomaster.core.search.*


abstract class Mutator<T> where T : Individual{

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var ff : FitnessFunction<T>


    abstract fun mutate(individual: T) : T


    fun mutate(upToNTimes: Int, individual: EvaluatedIndividual<T>, archive: Archive<T>) : Int{

        var current = individual

        for(i in 0 until upToNTimes){
            val mutated = ff.calculateCoverage(mutate(current.individual))

            if(current.fitness.subsumes(mutated.fitness)){
                continue
            }

            archive.addIfNeeded(mutated)

            current = mutated
        }

        return upToNTimes
    }
}