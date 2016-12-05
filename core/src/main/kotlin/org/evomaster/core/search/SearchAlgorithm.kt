package org.evomaster.core.search

import com.google.inject.Inject


abstract class SearchAlgorithm<T> where T : Individual{

    @Inject
    protected lateinit var sampler : Sampler<T>

    @Inject
    protected lateinit var ff : FitnessFunction<T>

    @Inject
    protected lateinit var randomness : Randomness


    abstract fun search(iterations: Int) : Solution<T>

}
