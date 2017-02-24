package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution


abstract class SearchAlgorithm<T> where T : Individual {

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

    @Inject
    protected lateinit var config: EMConfig


    @Inject
    private lateinit var mutator: Mutator<T>


    protected fun getMutatator() : Mutator<T>{
        return mutator
    }

    abstract fun search() : Solution<T>

    abstract fun getType() : EMConfig.Algorithm
}
