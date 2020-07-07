package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


class DisjunctionListRxGene(
        val disjunctions: List<DisjunctionRxGene>
) : RxAtom("disjunction_list") {

    var activeDisjunction: Int = 0

    init {
        for(d in disjunctions){
            d.parent = this
        }
    }

    companion object{
        private const val PROB_NEXT = 0.1
    }


    override fun copy(): Gene {
        val copy = DisjunctionListRxGene(disjunctions.map { it.copy() as DisjunctionRxGene })
        copy.activeDisjunction = this.activeDisjunction
        return copy
    }

    override fun isMutable(): Boolean {
        return disjunctions.size > 1 || disjunctions.any { it.isMutable() }
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        /*
            randomize content of all disjunctions
            (since standardMutation can be invoked on another term)
         */
        disjunctions.forEach {  it.randomize(randomness,forceNewValue,allGenes) }

        /**
         * randomly choose a new disjunction term
         */
        if (disjunctions.size > 1) {
            activeDisjunction = randomness.nextInt(0, disjunctions.size-1)
        }
    }

    // TODO Man need to check
    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        if(disjunctions.size > 1
                && (!disjunctions[activeDisjunction].isMutable() || randomness.nextBoolean(PROB_NEXT))){
            //activate the next disjunction
            return emptyList()
        } else {
            return listOf(disjunctions[activeDisjunction])
        }
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        TODO("NOT IMPLEMENTED")
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {
        //activate the next disjunction
        activeDisjunction = (activeDisjunction + 1) % disjunctions.size
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        if (disjunctions.isEmpty()) {
            return ""
        }
        return disjunctions[activeDisjunction]
                .getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DisjunctionListRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        this.activeDisjunction = other.activeDisjunction
        for (i in 0 until disjunctions.size) {
            this.disjunctions[i].copyValueFrom(other.disjunctions[i])
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisjunctionListRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (this.activeDisjunction != other.activeDisjunction) {
            return false
        }

        return this.disjunctions[activeDisjunction]
                .containsSameValueAs(other.disjunctions[activeDisjunction])
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(disjunctions.flatMap { it.flatView(excludePredicate) })
    }

    override fun mutationWeight(): Double = disjunctions.map { it.mutationWeight() }.sum() * PROB_NEXT + 1
}