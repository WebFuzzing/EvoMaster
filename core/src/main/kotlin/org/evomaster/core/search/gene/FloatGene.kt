package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class FloatGene(name: String,
                var value: Float = 0.0f
) : Gene(name) {

    override fun copy() = FloatGene(name, value)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        //need for forceNewValue?
        value = randomness.nextFloat()
    }

    override fun getValueAsPrintableString(): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene){
        if(other !is FloatGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }
}