package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * A gene that might or might not be active.
 * An example are for query parameters in URLs
 */
class OptionalGene(name: String,
                   val gene: Gene,
                   var isActive: Boolean = true)
    : Gene(name) {


    override fun copy(): Gene {
        return OptionalGene(name, gene.copy(), isActive)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is OptionalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.isActive = other.isActive
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is OptionalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isActive == other.isActive
                && this.gene.containsSameValueAs(other.gene)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {


        if (!forceNewValue) {
            isActive = randomness.nextBoolean()
            gene.randomize(randomness, false, allGenes)
        } else {

            if (randomness.nextBoolean()) {
                isActive = !isActive
            } else {
                gene.randomize(randomness, true, allGenes)
            }
        }
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if (!isActive) {
            isActive = true
        } else {

            if (randomness.nextBoolean(0.01)) {
                isActive = false
            } else {
                gene.standardMutation(randomness, apc, allGenes)
            }
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return gene.getValueAsPrintableString(targetFormat = targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }
}