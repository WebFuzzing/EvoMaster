package org.evomaster.core.search.gene.interfaces

/**
 * A gene representing possible different examples provided by the user.
 * Such examples might have unique names/ids used to easily identify them
 */
interface NamedExamplesGene {

    fun getValueName(): String?
}