package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class ByteGene(
        name: String,
        var value: Byte = 0,
        /** Inclusive */
        val min: Byte = Byte.MIN_VALUE,
        /** Inclusive */
        val max: Byte = Byte.MAX_VALUE
) : Gene(name) {


    override fun copy(): Gene {
        val copy = ByteGene(name, value, min, max)
        return copy
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ByteGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        val z: Byte = 100
        val range = max.toLong() - min.toLong() + 1L

        val a: Byte
        val b: Byte

        if (range > z && randomness.nextBoolean(0.95)) {
            //if very large range, might want to sample small values around 0 most of the times
            if (min <= 0 && max >= z) {
                a = 0
                b = z
            } else if (randomness.nextBoolean()) {
                a = min
                b = (min + z).toByte()
            } else {
                a = (max - z).toByte()
                b = max
            }
        } else {
            a = min
            b = max
        }

        value = if (forceNewValue) {
            randomness.nextInt(a.toInt(), b.toInt(), value.toInt()).toByte()
        } else {
            randomness.nextInt(a.toInt(), b.toInt()).toByte()
        }

    }

    override fun getValueAsPrintableString(): String {
        return value.toString()
    }
}