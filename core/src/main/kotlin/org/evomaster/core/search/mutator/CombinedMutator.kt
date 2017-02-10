package org.evomaster.core.search.mutator

import com.google.inject.Inject
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Mutator

/*
    TODO Choose at random, adaptively,
    eg start with high RandomMutator, and "slow" down to GreedyMutator
 */
class CombinedMutator  <T> : Mutator<T>() where T: Individual {

    @Inject
    private lateinit var randomMutator: RandomMutator<T>

    @Inject
    private lateinit var greedyMutator: GreedyMutator<T>


    override fun mutate(individual: T): T {

        return if(randomness.nextBoolean(0.1)){
            randomMutator.mutate(individual)
        } else {
            greedyMutator.mutate(individual)
        }
    }
}