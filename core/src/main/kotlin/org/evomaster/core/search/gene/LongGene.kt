package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class LongGene (
        name: String,
        var value: Long = 0
) : Gene(name) {


    override fun copy(): Gene {
        val copy = LongGene(name, value)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        var k = if(randomness.nextBoolean()) {
            randomness.nextLong()
        } else {
            randomness.nextInt() as Long
        }

        while(k == value){
            k = randomness.nextLong()
        }

        value = k
    }

    override fun getValueAsString() : String{
        return value.toString()
    }
}