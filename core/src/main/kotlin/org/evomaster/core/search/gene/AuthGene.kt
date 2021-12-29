package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.Randomness

class AuthGene(name: String, val gene: Gene, var employedAuth: Boolean) : Gene(name, mutableListOf(gene)) {

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        // randomize gene only if the employedAuth is false
        if (!employedAuth)
            gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return gene.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is AuthGene)
            throw IllegalStateException("Invalid gene type ${other.javaClass}")
        this.employedAuth = other.employedAuth
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is AuthGene)
            throw IllegalStateException("Invalid gene type ${other.javaClass}")
        return this.employedAuth == other.employedAuth &&
                this.gene.containsSameValueAs(other.gene)
    }

    override fun innerGene(): List<Gene> {
        return listOf(gene)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        throw IllegalStateException("auth gene should not bind value based on other gene")
    }

    override fun getChildren(): List<out StructuralElement> {
        return listOf(gene)
    }

    override fun isMutable(): Boolean = false

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun getVariableName() = gene.getVariableName()
}