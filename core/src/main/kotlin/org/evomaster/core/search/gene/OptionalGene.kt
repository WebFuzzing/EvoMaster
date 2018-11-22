package org.evomaster.core.search.gene

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

    override fun copyValueFrom(other: Gene){
        if(other !is OptionalGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.isActive = other.isActive
        this.gene.copyValueFrom(other.gene)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        if(! forceNewValue){
            isActive = randomness.nextBoolean()
            gene.randomize(randomness, false)
        } else {

            if(randomness.nextBoolean()){
                isActive = ! isActive
            } else {
                gene.randomize(randomness, true)
            }
        }
    }

    override fun getValueAsPrintableString() : String{
        return gene.getValueAsPrintableString()
    }

    override fun getValueAsRawString() : String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()

    override fun flatView(): List<Gene>{
        return listOf(this).plus(gene.flatView())
    }

}