package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A variable-length individual will be composed by 1 or more "actions".
 * Actions can be: REST call, setup Wiremock, setup database, etc.
 */
abstract class Action(children: List<out StructuralElement>) : StructuralElement(children){

    companion object{
        private val log: Logger = LoggerFactory.getLogger(Action::class.java)
    }

    abstract fun getName() : String

    /**
     * Return a view of the genes in the action.
     * Those are the actual instances, and not copies.
     */
    abstract fun seeGenes() : List<out Gene>

    final override fun copy() : Action{
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

    abstract fun randomize(
        randomness: Randomness,
        forceNewValue: Boolean,
        all: List<Action> = listOf())

}