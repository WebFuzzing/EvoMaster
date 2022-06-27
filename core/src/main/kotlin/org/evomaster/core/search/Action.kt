package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A variable-length individual will be composed by 1 or more "actions".
 * Actions can be: REST call, setup Wiremock, setup database, etc.
 */
abstract class Action(children: List<StructuralElement>) : StructuralElement(children.toMutableList()) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Action::class.java)
    }

    abstract fun getName(): String

    /**
     * Return a view of the genes in the action.
     * Those are the actual instances, and not copies.
     *
     * TODO clarify if these are top-level (i guess?) or not
     */
    abstract fun seeGenes(): List<out Gene>

    final override fun copy(): Action {
        val copy = super.copy()
        if (copy !is Action)
            throw IllegalStateException("mismatched type: the type should be Action, but it is ${this::class.java.simpleName}")
        return copy as Action
    }

    /**
     * Some actions can be expensive, like doing an HTTP call.
     * In those cases, a "longer" test would be more expensive to run.
     * However, there might also be setup actions that are not expensive,
     * eg like setting up a WireMock stub.
     */
    abstract fun shouldCountForFitnessEvaluations(): Boolean

    open fun postRandomizedChecks(randomness: Randomness?) {}

    /**
     * Randomize all genes in this action.
     */
    fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Gene> = listOf()) {
        seeGenes()
                .filter { it.isMutable() }
                .forEach {
                    it.randomize(randomness, forceNewValue, all)
                }
        postRandomizedChecks(randomness)
    }

    /**
     * Initialize all the genes in this action
     */
    fun doInitialize(randomness: Randomness? = null) {
        seeGenes().forEach { it.doInitialize(randomness) }
        postRandomizedChecks(randomness)
    }

    fun isInitialized(): Boolean {
        return seeGenes().all { it.initialized }
    }

    /**
     * removing all binding which refers to [this] gene
     */
    fun removeThisFromItsBindingGenes() {
        seeGenes().forEach { g ->
            g.flatView().forEach { r ->
                r.removeThisFromItsBindingGenes()
            }
        }
    }

}