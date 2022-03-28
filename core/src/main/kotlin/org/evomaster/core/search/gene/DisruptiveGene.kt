package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.value.DisruptiveGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * A gene that has a major, disruptive impact on the whole chromosome.
 * As such, it should be mutated only with low probability
 */
class DisruptiveGene<out T>(name: String, val gene: T, var probability: Double) : Gene(name, mutableListOf(gene))
        where T : Gene {

    init {
        if (probability < 0 || probability > 1) {
            throw IllegalArgumentException("Invalid probability value: $probability")
        }
        if (gene is DisruptiveGene<*>) {
            throw IllegalArgumentException("Cannot have a recursive disruptive gene")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(DisruptiveGene::class.java)
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(gene)

    override fun copyContent(): Gene {
        return DisruptiveGene(name, gene.copyContent(), probability)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return if(randomness.nextBoolean(probability)){
           listOf(gene)
        }else emptyList()
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DisruptiveGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not DisruptiveGeneImpact")
    }

    /**
     *  mutation of inside gene in DisruptiveGene is based on the probability.
     *  In [candidatesInternalGenes], we decide whether to return the inside gene .
     *  if the return is empty, [mutate] will be invoked
     */
    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        // do nothing due to rand() > probability
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun isMutable() = probability > 0

    override fun copyValueFrom(other: Gene) {
        if (other !is DisruptiveGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
        this.probability = other.probability
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisruptiveGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        /**
         * Not sure if probability is part of the
         * value for this gene
         */
        return this.gene.containsSameValueAs(other.gene)
                && this.probability == other.probability
    }


    override fun getVariableName() = gene.getVariableName()

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if(excludePredicate(this)) listOf(this) else listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight() * probability
    }

    override fun innerGene(): List<Gene> = listOf(gene)

    override fun possiblySame(gene: Gene): Boolean {
        return gene is DisruptiveGene<*> && gene.name == this.name && this.gene.possiblySame((gene as DisruptiveGene<T>).gene)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return ParamUtil.getValueGene(this).bindValueBasedOn(ParamUtil.getValueGene(gene))
    }
}