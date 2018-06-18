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

    override fun copyValueFrom(other: Gene){
        if(other !is IntegerGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        val z = 1000
        val range = max.toLong() - min.toLong() + 1L

        val a: Int
        val b: Int

        if(range > z && randomness.nextBoolean(0.95)){
            //if very large range, might want to sample small values around 0 most of the times
            if(min <= 0 && max >= z){
                a = 0
                b = z
            } else if(randomness.nextBoolean()){
                a = min
                b = min + z
            } else {
                a = max - z
                b = max
            }
        } else {
            a = min
            b = max
        }

        value = if (forceNewValue) {
            randomness.nextInt(a, b, value)
        } else {
            randomness.nextInt(a, b)
        }

    }

    override fun getValueAsPrintableString() : String{
        return value.toString()
    }
}