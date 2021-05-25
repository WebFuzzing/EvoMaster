package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory


abstract class Sampler<T> : TrackOperator where T : Individual {

    @Inject
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var config: EMConfig

    @Inject
    protected lateinit var time : SearchTimeController

    /**
     * Set of available actions that can be used to define a test case
     *
     * Key -> action name
     *
     * Value -> an action
     */
    protected val actionCluster: MutableMap<String, Action> = mutableMapOf()

    abstract fun sampleAtRandom(): T


    fun numberOfDistinctActions() = actionCluster.size

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Sampler::class.java)
    }


    /**
     * Create a new individual. Usually each call to this method
     * will create a new, different individual, but there is no
     * hard guarantee
     */
    fun sample(): T {
        if (log.isTraceEnabled){
            log.trace("sampler will be applied")
        }

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

    /**
     * this can be used to provide feedback to sampler regarding a fitness of the sampled individual (i.e., [evi]).
     */
    open fun feedback(evi : EvaluatedIndividual<T>){}
}