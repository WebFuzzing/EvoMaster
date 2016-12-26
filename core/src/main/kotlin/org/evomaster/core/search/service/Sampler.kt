package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual


abstract class Sampler<T> where T : Individual {

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var configuration: EMConfig


    protected val actionCluster: MutableMap<String, Action> = mutableMapOf()

    abstract fun sampleAtRandom() : T


    fun seeAvailableActions() : List<Action>{

        return actionCluster.entries
                .asSequence()
                .sortedBy { e -> e.key }
                .map { e -> e.value }
                .toList()
    }
}