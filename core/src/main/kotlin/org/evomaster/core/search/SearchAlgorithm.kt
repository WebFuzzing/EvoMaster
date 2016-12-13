package org.evomaster.core.search

import com.google.inject.Inject


abstract class SearchAlgorithm<T> where T : Individual{

    @Inject
    protected lateinit var sampler : Sampler<T>

    @Inject
    protected lateinit var ff : FitnessFunction<T>

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var time : SearchTimeController

    @Inject
    protected lateinit var archive: Archive<T>

    @Inject
    protected lateinit var apc: AdaptiveParameterControl


    abstract fun search() : Solution<T>

}
