package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-10
 *
 * @property mutatedGenes records what genes are mutated
 * @property addedGenes records what genes are added using structure mutator
 * @property removedGene records what genes are removed using structure mutator
 * @property mutatedPosition records where mutated/added/removed genes are located. but regarding different individual,
 * the position may be parsed in different way. For instance, the position may indicate the position of resource calls,
 * not rest action.
 */
open class MutatedGeneSpecification (
        val mutatedGenes : MutableList<Gene> = mutableListOf(),
        val addedGenes : MutableList<Gene> = mutableListOf(),
        val removedGene: MutableList<Gene> = mutableListOf(),
        val mutatedPosition : MutableList<Int> = mutableListOf()
)