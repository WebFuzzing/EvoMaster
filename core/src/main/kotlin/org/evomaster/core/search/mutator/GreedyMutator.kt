package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual

/*
    TODO variant of AVM, see recent McMinn and code at:

    https://github.com/AVMf/avmf/blob/master/src/main/java/org/avmframework/localsearch/LatticeSearch.java
    http://mcminn.io/publications/j17.pdf
 */
class GreedyMutator <T> : Mutator<T>() where T: Individual {

    override fun mutate(individual: T): T {
        throw UnsupportedOperationException("not implemented")
    }


}