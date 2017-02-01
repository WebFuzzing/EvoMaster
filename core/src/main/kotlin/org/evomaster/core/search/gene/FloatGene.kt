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

    override fun getValueAsString(): String {
        return value.toString()
    }

}