package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class DoubleGene(name: String,
                 var value: Double = 0.0
) : Gene(name) {

    override fun copy() = DoubleGene(name, value)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        //need for forceNewValue?
        value = randomness.nextDouble()
    }

    override fun getValueAsPrintableString(): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene){
        if(other !is DoubleGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }
}