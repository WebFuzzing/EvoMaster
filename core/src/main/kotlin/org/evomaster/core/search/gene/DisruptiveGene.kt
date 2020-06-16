package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactInfoCollection.value.DisruptiveGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * A gene that has a major, disruptive impact on the whole chromosome.
 * As such, it should be mutated only with low probability
 */
class DisruptiveGene<out T>(name: String, val gene: T, var probability: Double) : Gene(name)
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

    init {
        gene.parent = this
    }


    override fun copy(): Gene {
        return DisruptiveGene(name, gene.copy(), probability)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return if(randomness.nextBoolean(probability)){
           listOf(gene)
        }else emptyList()
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DisruptiveGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact))
        }
        throw IllegalArgumentException("impact is null or not DisruptiveGeneImpact")
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {
        // do nothing due to rand() > probability
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
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

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, Int>, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is DisruptiveGene<*>){
                log.warn("original ({}) should be DisruptiveGene", original::class.java.simpleName)
                return
            }
            if (mutated !is DisruptiveGene<*>){
                log.warn("mutated ({}) should be DisruptiveGene", mutated::class.java.simpleName)
                return
            }
            gene.archiveMutationUpdate(original.gene, mutated.gene, targetsEvaluated, archiveMutator)
        }
    }

    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight() * probability
    }
}