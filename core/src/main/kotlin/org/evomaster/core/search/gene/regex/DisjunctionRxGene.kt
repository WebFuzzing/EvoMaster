package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.regex.DisjunctionRxGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DisjunctionRxGene(
        name: String,
        val terms: List<RxTerm>,
        /**  does this disjunction match the beginning of the string, or could it be at any position? */
        var matchStart: Boolean,
        /** does this disjunction match the end of the string, or could it be at any position? */
        var matchEnd: Boolean
) : RxAtom(name, terms.toMutableList()) {

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

    companion object{
        private const val APPEND = 0.05
        private val log : Logger = LoggerFactory.getLogger(DisjunctionRxGene::class.java)
    }

    override fun getChildren(): List<RxTerm> = terms

    override fun copyContent(): Gene {
        val copy = DisjunctionRxGene(name, terms.map { it.copyContent() as RxTerm }, matchStart, matchEnd)
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

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return if(!matchStart && randomness.nextBoolean(APPEND)){
            emptyList()
        } else if(!matchEnd && randomness.nextBoolean(APPEND)){
            emptyList()
        } else {
            terms.filter { it.isMutable() }
        }
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact == null || additionalGeneMutationInfo.impact !is DisjunctionRxGeneImpact)
            throw IllegalArgumentException("mismatched gene impact")

        if (!terms.containsAll(internalGenes))
            throw IllegalArgumentException("mismatched internal genes")

        val impacts = internalGenes.map {
            additionalGeneMutationInfo.impact.termsImpact[terms.indexOf(it)]
        }

        val selected = mwc.selectSubGene(
                candidateGenesToMutate = internalGenes,
                impacts = impacts,
                targets = additionalGeneMutationInfo.targets,
                forceNotEmpty = true,
                adaptiveWeight = true
        )
        return selected.map { it to additionalGeneMutationInfo.copyFoInnerGene(impacts[internalGenes.indexOf(it)], it) }.toList()
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if(!matchStart){
            extraPrefix = ! extraPrefix
        } else {
            extraPostfix = ! extraPostfix
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

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

        //TODO Man: Andrea, please check this code
        if (terms.size != other.terms.size) return false

        //Man: if terms is empty, there throws IndexOutOfBoundsException (found by rest-scs case study)
        if (terms.isNotEmpty()){
            for (i in 0 until terms.size) {
                if ( this.terms[i]::class.java.simpleName != other.terms[i]::class.java.simpleName ||!this.terms[i].containsSameValueAs(other.terms[i])) {
                    return false
                }
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

    override fun innerGene(): List<Gene> = terms


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is DisjunctionRxGene && terms.size == gene.terms.size){
            var result = true
            terms.indices.forEach { i->
                val r = terms[i].bindValueBasedOn(gene.terms[i])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind the term (name: ${terms[i].name}) at index $i")
                result = result && r
            }

            extraPostfix = gene.extraPrefix
            extraPrefix = gene.extraPrefix

            if (!result){
                LoggingUtil.uniqueWarn(log, "not fully completely bind DisjunctionRxGene")
            }
            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind DisjunctionRxGene with ${gene::class.java.simpleName}")
        return false
    }
}