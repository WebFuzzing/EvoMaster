package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene

/**
 * A variable-length individual will be composed by 1 or more "actions".
 * Actions can be: REST call, setup Wiremock, setup database, etc.
 */
interface  Action {

    fun getName() : String

    /**
     * Return a view of the genes in the action.
     * Those are the actual instances, and not copies.
     */
    fun seeGenes() : List<out Gene>

    fun copy() : Action

    /**
     * Some actions can be expensive, like doing an HTTP call.
     * In those cases, a "longer" test would be more expensive to run.
     * However, there might also be setup actions that are not expensive,
     * eg like setting up a WireMock stub.
     */
    fun shouldCountForFitnessEvaluations(): Boolean
}