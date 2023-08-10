package org.evomaster.core.search.action

import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A variable-length individual will be composed by 1 or more "actions".
 * Actions can be: REST call, setup Wiremock, setup database, etc.
 */
abstract class Action(children: List<StructuralElement>) : ActionComponent(
    children.toMutableList(),
    {k -> !ActionComponent::class.java.isAssignableFrom(k)
            && !Individual::class.java.isAssignableFrom(k)} //action should not have other actions nor individuals
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Action::class.java)
    }

    override fun flatten(): List<Action> {
        return listOf(this)
    }

    abstract fun getName(): String

    /**
     * Return a view of the top genes in the action.
     * Those are the actual instances, and not copies.
     *
     * A top gene is at the root of a gene tree.
     * Note that top gene might not be mounted directly under an Action, as there can
     * be other structural elements in between, like Param for REST.
     * However, these intermediate structures should only impact the phenotype, and not
     * the genotype
     */
    abstract fun seeTopGenes(): List<out Gene>

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
    fun randomize(randomness: Randomness, forceNewValue: Boolean) {
        seeTopGenes()
                .filter { it.isMutable() }
                .forEach {
                    it.randomize(randomness, forceNewValue)
                }
        postRandomizedChecks(randomness)
    }

    /**
     * Initialize all the genes in this action
     */
    open fun doInitialize(randomness: Randomness? = null) {
        seeTopGenes().forEach { it.doInitialize(randomness) }
        postRandomizedChecks(randomness)
    }

    fun isInitialized(): Boolean {
        return seeTopGenes().all { it.initialized }
    }

    /**
     * removing all binding which refers to [this] gene
     */
    fun removeThisFromItsBindingGenes() {
        seeTopGenes().forEach { g ->
            g.flatView().forEach { r ->
                r.removeThisFromItsBindingGenes()
            }
        }
    }

    override fun flatView(): List<ActionComponent> {
        return listOf(this)
    }

}