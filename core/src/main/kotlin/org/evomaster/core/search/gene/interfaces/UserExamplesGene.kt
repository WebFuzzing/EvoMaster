package org.evomaster.core.search.gene.interfaces

import org.evomaster.core.search.gene.Gene

/**
 * A gene representing possible different examples provided by the user.
 * Such examples might have unique names/ids used to easily identify them.
 *
 * Note: a gene that might be used for User-Examples does not mean that necessarily has user examples.
 *
 * This interface must be used only on Gene classes
 */
interface UserExamplesGene {

    companion object {
        /**
         * Name given to enum genes representing data examples coming from OpenAPI schema
         */
        const val EXAMPLES_NAME = "SCHEMA_EXAMPLES"
    }

    /**
     * Check if this gene is used to store examples provided by the user
     */
    fun isUsedForExamples() : Boolean{
        return (this as Gene).name == EXAMPLES_NAME
    }

    /**
     * The name of the chosen example, which is selected for the value of this gene.
     * If there are no examples defined for this gene, or the chosen example is unnamed, then this function returns null.
     */
    fun getValueName(): String?

}

