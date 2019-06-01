package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import java.lang.IllegalArgumentException


class DisjunctionRxGene(
        name: String,
        val terms : List<RxTerm>
) : Gene(name) {

    override fun copy(): Gene {
        return DisjunctionRxGene(name, terms.map { it.copy() as RxTerm})
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
       terms.filter { it.isMutable() }
               .forEach{it.randomize(randomness, forceNewValue, allGenes)}
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return terms.map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                .joinToString("")
    }

    override fun copyValueFrom(other: Gene) {
        if(other !is DisjunctionRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for(i in 0 until terms.size){
            this.terms[i].copyValueFrom(other.terms[i])
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is DisjunctionRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for(i in 0 until terms.size){
            if(! this.terms[i].containsSameValueAs(other.terms[i])){
                return false
            }
        }
        return true
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf()
        else listOf(this).plus(terms.flatMap { it.flatView(excludePredicate) })
    }
}