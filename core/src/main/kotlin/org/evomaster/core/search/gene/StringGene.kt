package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class StringGene (
        name: String,
        var value: String = "foo",
        /** Inclusive */
        val minLength: Int = 0,
        /** Inclusive */
        val maxLength: Int = 10
) : Gene(name) {


    override fun copy(): Gene {
        val copy = StringGene(name, value, minLength, maxLength)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

//        val k = if (forceNewValue) {
//            randomness.nextInt(min, max, value)
//        } else {
//            randomness.nextInt(min, max)
//        }
//
//        value = k
        //TODO
    }

    override fun getValueAsString() : String{
        return value
    }
}