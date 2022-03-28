package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.Randomness


/**
 *  A gene representing existing data that cannot be modified,
 *  nor be directly used in a test action.
 *  However, it can be indirectly referred to.
 *
 *  A typical example is a Primary Key in a database, and we want
 *  a Foreign Key pointing to it
 */
class ImmutableDataHolderGene(name: String, val value: String, val inQuotes: Boolean) : Gene(name, mutableListOf()){

    /*
        Note that instead of returning itself, here we create a copy for it since it might be bound with other genes.
        if we directly return as it is, the binding with genes in previous individual will be remained.
        Since the value is not mutable, it should not have further side-effect
     */
    override fun copyContent(): Gene {
        return ImmutableDataHolderGene(name, value, inQuotes) // recall it is immutable
    }

    override fun isMutable() = false

    override fun isPrintable() = true

    override fun getChildren(): MutableList<Gene> = mutableListOf()

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if(inQuotes){
            return "\"$value\""
        }
        return value
    }

    override fun copyValueFrom(other: Gene) {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is ImmutableDataHolderGene){
            return false
        }
        return value == other.value
    }

    override fun mutationWeight(): Double = 0.0

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        // do nothing
        return true
    }

}