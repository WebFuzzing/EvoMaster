package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness

/**
 * A gene that has a major, disruptive impact on the whole chromosome.
 * As such, it should be mutated only with low probability
 */
class DisruptiveGene<out T>(name: String, val gene: T, var probability: Double) : Gene(name)
    where T: Gene{

    init {
        if (probability < 0 || probability > 1){
            throw IllegalArgumentException("Invalid probability value: $probability")
        }
        if(gene is DisruptiveGene<*>){
            throw IllegalArgumentException("Cannot have a recursive disruptive gene")
        }
    }

    override fun copy(): Gene {
        return DisruptiveGene(name, gene.copy(), probability)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {
        gene.randomize(randomness, forceNewValue)
    }

    override fun getValueAsPrintableString(): String {
        return gene.getValueAsPrintableString()
    }

    override fun getValueAsRawString() : String {
        return gene.getValueAsRawString()
    }

    override fun isMutable() = probability > 0

    override fun copyValueFrom(other: Gene){
        if(other !is DisruptiveGene<*>){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
        this.probability = other.probability
    }

    override fun getVariableName() = gene.getVariableName()

    override fun flatView(): List<Gene>{
        return listOf(this).plus(gene.flatView())
    }

}