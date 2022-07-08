package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gene that holds many potenial genes (genotype) but
 * only one is active at any time (phenotype). The list
 * of gene choices cannot be empty.
 */

class ChoiceGene<T>(name: String,
                    private val geneChoices: List<T>,
                    activeChoice: Int = 0

) : CompositeFixedGene(name, geneChoices) where T : Gene {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ChoiceGene::class.java)
    }

    var activeGeneIndex: Int = activeChoice
        private set

    init {
        if (geneChoices.isEmpty()) {
            throw IllegalArgumentException("The list of gene choices cannot be empty")
        }

        if (activeChoice < 0 || activeChoice >= geneChoices.size) {
            throw IllegalArgumentException("Active choice must be between 0 and ${geneChoices.size - 1}")
        }
    }

    /**
     * Randomizes the active gene index, and if the newly selected active gene index
     * is mutable, the active gene is also randomized.
     */
    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        activeGeneIndex = randomness.nextInt(geneChoices.size)
        if (geneChoices[activeGeneIndex].isMutable()) {
            geneChoices[activeGeneIndex].randomize(randomness, tryToForceNewValue, allGenes)
        }
    }

    /**
     * TODO This method must be implemented to reflect usage
     * of the selectionStrategy and the additionalGeneMutationInfo
     */
    override fun candidatesInternalGenes(randomness: Randomness,
                                         apc: AdaptiveParameterControl,
                                         allGenes: List<Gene>,
                                         selectionStrategy: SubsetGeneSelectionStrategy,
                                         enableAdaptiveGeneMutation: Boolean,
                                         additionalGeneMutationInfo: AdditionalGeneMutationInfo?) = innerGene()

    /**
     * Returns only the active gene.
     */
    override fun innerGene() =
            listOf(geneChoices[activeGeneIndex])


    /**
     * Returns the value of the active gene as a printable string
     */
    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return geneChoices[activeGeneIndex]
                .getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    /**
     * Returns the value of the active gene as a raw string
     */
    override fun getValueAsRawString(): String {
        return geneChoices[activeGeneIndex]
                .getValueAsRawString()
    }

    /**
     * Copies the value of the other gene. The other gene
     * has to be a [ChoiceGene] with the same number
     * of gene choices. The value of each gene choice
     * is also copied.
     */
    override fun copyValueFrom(other: Gene) {
        if (other !is ChoiceGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        } else if (geneChoices.size != other.geneChoices.size) {
            throw IllegalArgumentException("Cannot copy value from another choice gene with  ${other.geneChoices.size} choices (current gene has ${geneChoices.size} choices)")
        } else {
            this.activeGeneIndex = other.activeGeneIndex
            for (i in geneChoices.indices) {
                this.geneChoices[i].copyValueFrom(other.geneChoices[i])
            }
        }
    }

    /**
     * Checks that the other gene is another ChoiceGene,
     * the active gene index is the same, and the gene choices are the same.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ChoiceGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.activeGeneIndex != other.activeGeneIndex) {
            return false
        }

        return this.geneChoices[activeGeneIndex]
                .containsSameValueAs(other.geneChoices[activeGeneIndex])
    }

    /**
     * Binds this gene to another [ChoiceGene<T>] with the same number of
     * gene choices, one gene choice to the corresponding gene choice in
     * the other gene.
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is ChoiceGene<*> && gene.geneChoices.size == geneChoices.size) {
            var result = true
            geneChoices.indices.forEach { i ->
                val r = geneChoices[i].bindValueBasedOn(gene.geneChoices[i])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind disjunctions (name: ${geneChoices[i].name}) at index $i")
                result = result && r
            }

            activeGeneIndex = gene.activeGeneIndex
            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind ChoiceGene with ${gene::class.java.simpleName}")
        return false
    }

    /**
     * Returns a copy of this gene choice by copying
     * all gene choices.
     */
    override fun copyContent(): Gene = ChoiceGene(
            name,
            activeChoice = this.activeGeneIndex,
            geneChoices = this.geneChoices.map { it.copy() }.toList()
    )


}