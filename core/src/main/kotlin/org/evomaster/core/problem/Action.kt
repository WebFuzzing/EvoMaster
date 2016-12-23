package org.evomaster.core.problem

import org.evomaster.core.search.gene.Gene

/**
 * A variable-length individual will be composed by 1 or more "actions".
 * Actions can be: REST call, setup Wiremock, setup database, etc.
 */
interface  Action {

    fun getGenes() : List<out Gene>

    fun execute()
}