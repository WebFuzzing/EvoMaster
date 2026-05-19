package org.evomaster.core.search.gene.interfaces

import org.evomaster.core.search.gene.Gene


/**
 * A gene that might have internal genes that might not impact the phenotype, based on current status of this gene
 */
interface PhenotypeDormantGene {

    fun isChildActive(child: Gene): Boolean

    fun tryToActivateGene(child: Gene): Boolean
}