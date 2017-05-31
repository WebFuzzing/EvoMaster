package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual


abstract class Sampler<T> where T : Individual {

    @Inject
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var config: EMConfig

    /**
     * Set of available actions that can be used to define a test case
     *
     * Key -> action name
     *
     * Value -> an action
     */
    protected val actionCluster: MutableMap<String, Action> = mutableMapOf()

    abstract fun sampleAtRandom(): T


    /**
     * Create a new individual. Usually each call to this method
     * will create a new, different individual, but there is no
     * hard guarantee
     */
    fun sample(): T {
        if (randomness.nextBoolean(config.probOfSmartSampling)) {
            return smartSample()
        } else {
            return sampleAtRandom()
        }
    }

    /**
     * Create a new individual, but not fully at random, but rather
     * by using some domain-knowledge.
     */
    open fun smartSample(): T {
        //unless this method is overridden, just sample at random
        return sampleAtRandom()
    }

    /**
     * When the search starts, there might be some predefined individuals
     * that we can sample. But we just need to sample each of them just once.
     * The [smartSample] must first pick from this set.
     *
     * @return false if there is not left predefined individual to sample
     */
    open fun hasSpecialInit() = false


    open fun resetSpecialInit() {}


    fun seeAvailableActions(): List<Action> {

        return actionCluster.entries
                .asSequence()
                .sortedBy { e -> e.key }
                .map { e -> e.value }
                .toList()
    }
}