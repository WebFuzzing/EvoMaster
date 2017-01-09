package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class IntegerGene(
        name: String,
        var value: Int = 0,
        /** Inclusive */
        val min: Int = Int.MIN_VALUE,
        /** Inclusive */
        val max: Int = Int.MAX_VALUE
) : Gene(name) {


    override fun copy(): Gene {
        val copy = IntegerGene(name, value, min, max)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        val k = if (forceNewValue) {
            randomness.nextInt(min, max, value)
        } else {
            randomness.nextInt(min, max)
        }

        value = k
    }

    override fun getValueAsString() : String{
        return value.toString()
    }
}