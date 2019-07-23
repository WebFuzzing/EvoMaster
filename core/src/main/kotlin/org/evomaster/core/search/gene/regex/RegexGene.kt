package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * A gene representing a regular expression (regex).
 */
class RegexGene(
        name: String,
        val disjunctions: DisjunctionListRxGene
) : Gene(name) {

    override fun copy(): Gene {
        return RegexGene(name, disjunctions.copy() as DisjunctionListRxGene)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        disjunctions.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        disjunctions.standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsRawString(): String {
        return disjunctions.getValueAsPrintableString(targetFormat = null)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        val rawValue = getValueAsRawString()
        when {
            // TODO Should refactor since this code block is equivalent to StringGene.getValueAsPrintableString()
            (targetFormat == null) -> return "\"$rawValue\""
            targetFormat.isKotlin() -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
                    .replace("$", "\\$")
            else -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
        }
    }


    override fun copyValueFrom(other: Gene) {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.disjunctions.copyValueFrom(other.disjunctions)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.disjunctions.containsSameValueAs(other.disjunctions)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(disjunctions.flatView(excludePredicate))
    }
}