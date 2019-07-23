package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

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

    override fun copy(): Gene {
        return DisruptiveGene(name, gene.copy(), probability)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        if(randomness.nextBoolean(probability)){
            gene.standardMutation(randomness, apc, allGenes)
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
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

}