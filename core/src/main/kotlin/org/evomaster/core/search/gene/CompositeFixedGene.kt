package org.evomaster.core.search.gene

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.CompositeFixedGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo

abstract class CompositeFixedGene(
        name: String,
        children: List<out Gene>
) : CompositeGene(name, children.toMutableList()) {

    constructor(name: String, child: Gene) : this(name, mutableListOf(child))

    init {
        if(children.isEmpty() && !canBeChildless()){
            throw IllegalStateException("A fixed composite gene must have at least 1 internal gene")
        }
    }

    open fun canBeChildless() = false


    private val errorChildMsg = "BUG in EvoMaster: cannot modify children of fixed ${this.javaClass}"

    override fun addChild(child: StructuralElement) {
        throw IllegalStateException(errorChildMsg)
    }

    override fun addChild(position: Int, child: StructuralElement){
        throw IllegalStateException(errorChildMsg)
    }

    override fun addChildren(position: Int, list : List<StructuralElement>){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killAllChildren(){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChild(child: StructuralElement){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChildByIndex(index: Int) : StructuralElement{
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChildren(predicate: (StructuralElement) -> Boolean){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChildren(toKill: List<out StructuralElement>){
        throw IllegalStateException(errorChildMsg)
    }

    override fun adaptiveSelectSubset(
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
}