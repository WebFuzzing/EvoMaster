package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class IntegerGene(
        name: String,
        /** Inclusive */
        val min: Int,
        /** Inclusive */
        val max: Int,
        var value: Int
) : Gene(name) {

    constructor(name: String) : this(name, Int.MIN_VALUE, Int.MAX_VALUE, 0)

    override fun copy(): Gene {
        val copy = IntegerGene(name, min, max, value)
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