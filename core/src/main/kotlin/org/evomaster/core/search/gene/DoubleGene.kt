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

    override fun getValueAsString(): String {
        return value.toString()
    }

}