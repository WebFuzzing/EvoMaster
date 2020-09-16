package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy


class DisjunctionRxGene(
        name: String,
        val terms: List<RxTerm>,
        /**  does this disjunction match the beginning of the string, or could it be at any position? */
        var matchStart: Boolean,
        /** does this disjunction match the end of the string, or could it be at any position? */
        var matchEnd: Boolean
) : RxAtom(name) {

    /**
     * whether we should append a prefix.
     * this can only happen if [matchStart] is false
     */
    var extraPrefix = false

    /**
     * whether we should append a postfix.
     * this can only happen if [matchEnd] is false
     */
    var extraPostfix = false

    init {
        for(t in terms){
            t.parent = this
        }
    }

    companion object{
        private const val APPEND = 0.05
    }

    override fun copy(): Gene {
        val copy = DisjunctionRxGene(name, terms.map { it.copy() as RxTerm }, matchStart, matchEnd)
        copy.extraPrefix = this.extraPrefix
        copy.extraPostfix = this.extraPostfix
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        terms.filter { it.isMutable() }
                .forEach { it.randomize(randomness, forceNewValue, allGenes) }

        if (!matchStart) {
            extraPrefix = randomness.nextBoolean()
        }

        if (!matchEnd) {
            extraPostfix = randomness.nextBoolean()
        }
    }

    override fun isMutable(): Boolean {
        return !matchStart || !matchEnd || terms.any { it.isMutable() }
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return if(!matchStart && randomness.nextBoolean(APPEND)){
            emptyList()
        } else if(!matchEnd && randomness.nextBoolean(APPEND)){
            emptyList()
        } else {
            terms.filter { it.isMutable() }
        }
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        TODO()
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {
        if(!matchStart){
            extraPrefix = ! extraPrefix
        } else {
            extraPostfix = ! extraPostfix
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {

        val prefix = if (extraPrefix) "prefix_" else ""
        val postfix = if (extraPostfix) "_postfix" else ""

        return prefix +
                terms.map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                        .joinToString("") +
                postfix
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DisjunctionRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for (i in 0 until terms.size) {
            this.terms[i].copyValueFrom(other.terms[i])
        }
        this.extraPrefix = other.extraPrefix
        this.extraPostfix = other.extraPostfix
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisjunctionRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for (i in 0 until terms.size) {
            if (!this.terms[i].containsSameValueAs(other.terms[i])) {
                return false
            }
        }

        return this.extraPrefix == other.extraPrefix &&
                this.extraPostfix == other.extraPostfix
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(terms.flatMap { it.flatView(excludePredicate) })
    }

    override fun mutationWeight(): Double {
        return terms.filter { isMutable() }.map { it.mutationWeight() }.sum()
    }
}