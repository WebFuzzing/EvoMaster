package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class ShortGene(
        name: String,
        var value: Short = 0,
        /** Inclusive */
        val min: Short = Short.MIN_VALUE,
        /** Inclusive */
        val max: Short = Short.MAX_VALUE
) : Gene(name) {


    override fun copy(): Gene {
        val copy = ShortGene(name, value, min, max)
        return copy
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ShortGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        val z: Short = 100
        val range = max.toLong() - min.toLong() + 1L

        val a: Short
        val b: Short

        if (range > z && randomness.nextBoolean(0.95)) {
            //if very large range, might want to sample small values around 0 most of the times
            if (min <= 0 && max >= z) {
                a = 0
                b = z
            } else if (randomness.nextBoolean()) {
                a = min
                b = (min + z).toShort()
            } else {
                a = (max - z).toShort()
                b = max
            }
        } else {
            a = min
            b = max
        }

        value = if (forceNewValue) {
            randomness.nextInt(a.toInt(), b.toInt(), value.toInt()).toShort()
        } else {
            randomness.nextInt(a.toInt(), b.toInt()).toShort()
        }

    }

    override fun getValueAsPrintableString(): String {
        return value.toString()
    }
}