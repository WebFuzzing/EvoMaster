package org.evomaster.core.search.gene.interfaces

import org.evomaster.core.search.gene.Gene

/**
 * the gene implemented this interface could be merged with other genes based on [isMergeableWith]
 */
interface MergeableGene {

    /**
     * @return whether the current gene is mergeable with the given [gene]
     */
    fun isMergeableWith(gene: Gene) : Boolean
}