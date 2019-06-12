package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness


class DisjunctionRxGene(
        name: String,
        val terms: List<RxTerm>,
        val matchStart: Boolean,
        val matchEnd: Boolean
) : RxAtom(name) {

    var extraPrefix = false

    var extraPostfix = false

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

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {

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
        return if (excludePredicate(this)) listOf()
        else listOf(this).plus(terms.flatMap { it.flatView(excludePredicate) })
    }
}