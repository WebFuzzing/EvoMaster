package org.evomaster.core.search.mutator

import com.google.inject.Inject
import org.evomaster.core.search.Archive
import org.evomaster.core.search.FitnessFunction
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Randomness


abstract class Mutator<T> where T : Individual{

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var ff : FitnessFunction<T>


    abstract fun mutate(individual: T) : T


    fun mutate(upToNTimes: Int, individual: T, archive: Archive<T>) : Int{

        var mutated = individual

        for(i in 0 until upToNTimes){
            mutated = mutate(mutated)
            archive.addIfNeeded(ff.calculateCoverage(mutated))
        }

        return upToNTimes
    }
}