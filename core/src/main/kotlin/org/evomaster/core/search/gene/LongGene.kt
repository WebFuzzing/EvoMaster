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
            randomness.nextInt().toLong()
        }

        while(k == value){
            k = randomness.nextLong()
        }

        value = k
    }

    override fun getValueAsPrintableString() : String{
        return value.toString()
    }

    override fun copyValueFrom(other: Gene){
        if(other !is LongGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }
}