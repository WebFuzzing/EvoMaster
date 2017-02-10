package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Mutator

/*
    TODO Combine to mutator. Choose at random, adaptively,
    eg start with high RandomMutator, and "slow" down to GreedyMutator
 */
class CombinedMutator  <T> : Mutator<T>() where T: Individual {
    override fun mutate(individual: T): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}