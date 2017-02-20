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

        val z = 1000

        if(min < -z && max > z && randomness.nextBoolean()){
            //if very large range, might want to sample small values any now and then
            if (forceNewValue) {
                value = randomness.nextInt(-z, z, value)
            } else {
                value = randomness.nextInt(-z, z)
            }
        } else {
            if (forceNewValue) {
                value = randomness.nextInt(min, max, value)
            } else {
                value = randomness.nextInt(min, max)
            }
        }
    }

    override fun getValueAsString() : String{
        return value.toString()
    }
}