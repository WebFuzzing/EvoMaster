package org.evomaster.core.search.algorithms

import org.evomaster.core.search.Individual
import org.evomaster.core.search.SearchAlgorithm
import org.evomaster.core.search.Solution

/**
 * Advanced Whole Test Suite Algorithm
 */
class AwtsAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {


    private var archive : MutableList<T> = mutableListOf()

    private var evaluated = 0



    override fun search(iterations: Int): Solution<T> {

        while(evaluated < iterations){


        }

        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}