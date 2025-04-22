package org.evomaster.core.search.gene.placeholder

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 *  A gene representing existing data that cannot be modified,
 *  nor be directly used in a test action.
 *  However, it can be indirectly referred to.
 *
 *  A typical example is a Primary Key in a database, and we want
 *  a Foreign Key pointing to it
 */
class ImmutableDataHolderGene(
        name: String,
        val value: String,
        val inQuotes: Boolean
        ) : SimpleGene(name){

    /*
        Note that instead of returning itself, here we create a copy for it since it might be bound with other genes.
        if we directly return as it is, the binding with genes in previous individual will be remained.
        Since the value is not mutable, it should not have further side-effect
     */
    override fun copyContent(): Gene {
        return ImmutableDataHolderGene(name, value, inQuotes) // recall it is immutable
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for ImmutableDataHolderGene")
    }

    override fun isMutable() = false

    override fun isPrintable() = true

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if(inQuotes){
            return "\"$value\""
        }
        return value
    }

    override fun copyValueFrom(other: Gene): Boolean {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is ImmutableDataHolderGene){
            return false
        }
        return value == other.value
    }

    override fun mutationWeight(): Double = 0.0


    override fun setValueBasedOn(gene: Gene): Boolean {
        // do nothing
        return true
    }

}