package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.*
import org.evomaster.core.search.mutator.CombinedMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.mutator.RandomMutator
import org.evomaster.core.search.service.Randomness


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


    //following are private. shouldn't be accessed directly.
    //rather use "getMutator"

    @Inject
    private lateinit var randomMutator: RandomMutator<T>

    @Inject
    private lateinit var standardMutator: StandardMutator<T>

    @Inject
    private lateinit var combinedMutator: CombinedMutator<T>


    protected fun getMutatator() : Mutator<T>{
        return when(config.mutator){
            EMConfig.Mutators.RANDOM -> randomMutator
            EMConfig.Mutators.STANDARD -> standardMutator
            EMConfig.Mutators.COMBINED -> combinedMutator
            else -> throw IllegalStateException("Unrecognized mutator: ${config.mutator}")
        }
    }

    abstract fun search() : Solution<T>

}
