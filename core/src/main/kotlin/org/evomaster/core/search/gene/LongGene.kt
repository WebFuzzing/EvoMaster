package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.Randomness


class LongGene(
        name: String,
        value: Long = 0
) : NumberGene<Long>(name, value) {


    override fun copy(): Gene {
        val copy = LongGene(name, value)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        var k = if (randomness.nextBoolean()) {
            randomness.nextLong()
        } else {
            randomness.nextInt().toLong()
        }

        while (k == value) {
            k = randomness.nextLong()
        }

        value = k
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

}