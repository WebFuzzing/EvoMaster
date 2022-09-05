package org.evomaster.core.search.gene.optional

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
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
 *
 * @param probability of the gene can be mutated. 0 means it is impossible to mutate,
 *                      whereas 1 means can always be mutated
 */
class DisruptiveGene<out T>(name: String, val gene: T, var probability: Double
) : CompositeFixedGene(name, gene)
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

    override fun isLocallyValid() : Boolean{
        return getViewOfChildren().all { it.isLocallyValid() }
    }

    override fun copyContent(): Gene {
        return DisruptiveGene(name, gene.copy(), probability)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        gene.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneSelectionStrategy,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {

        /*
            This is a bit convoluted... but Disruptive gene is rather special.
            Whether it can be mutated is based on a probability, so it is not deterministic.
            Applying "shallow mutate" here actually means not mutating...
            So, the "higher" probability of mutate, the "lower" the probability of shallow mutate
         */

        return randomness.nextBoolean(1 - probability)
    }

    /**
     *  mutation of inside gene in DisruptiveGene is based on the probability.
     *  If not mutating internal gene, then shallow does nothing
     */
    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        // do nothing, but still not crash EM. this is an expected behaviour, see customShouldApplyShallowMutation
        return true
    }


    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DisruptiveGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not DisruptiveGeneImpact")
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun isMutable() = probability > 0 && gene.isMutable()

    override fun isPrintable() = gene.isPrintable()

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



    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight() * probability
    }


    override fun possiblySame(gene: Gene): Boolean {
        return gene is DisruptiveGene<*> && gene.name == this.name && this.gene.possiblySame((gene as DisruptiveGene<T>).gene)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return ParamUtil.getValueGene(this).bindValueBasedOn(ParamUtil.getValueGene(gene))
    }
}