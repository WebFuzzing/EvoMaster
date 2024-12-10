package org.evomaster.core.search.gene.root

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.CompositeFixedGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo

abstract class CompositeFixedGene(
        name: String,
        children: List<out Gene>
) : CompositeConditionalFixedGene(name, true, children.toMutableList()) {

    constructor(name: String, child: Gene) : this(name, mutableListOf(child))

    override fun canBeChildless() = false


    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null
            && additionalGeneMutationInfo.impact is CompositeFixedGeneImpact
        ) {
            val impacts = internalGenes.map {
                additionalGeneMutationInfo.impact.getChildImpact(getViewOfChildren().indexOf(it), it.name)?: throw IllegalStateException("cannot find the impact for ${it.name}")
            }

            val selected = mwc.selectSubGene(
                internalGenes, true, additionalGeneMutationInfo.targets, individual = null, impacts = impacts, evi = additionalGeneMutationInfo.evi
            )
            val map = selected.map { internalGenes.indexOf(it) }
            return map.map { internalGenes[it] to additionalGeneMutationInfo.copyFoInnerGene(impact = impacts[it] as? GeneImpact, gene = internalGenes[it]) }
        }
        throw IllegalArgumentException("impact is null or not CompositeFixedGeneImpact, ${additionalGeneMutationInfo.impact}")
    }

    /**
     * @return length of children for the CompositeFixedGene
     */
    fun lengthOfChildren() : Int = children.size
}