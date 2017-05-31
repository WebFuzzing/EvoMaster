package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class EnumGene<T>(
        name: String,
        val values: List<T>,
        var index: Int = 0
) : Gene(name) {

    init {
        if (values.isEmpty()) {
            throw IllegalArgumentException("Empty list of values")
        }
        if (index < 0 || index >= values.size) {
            throw IllegalArgumentException("Invalid index: " + index)
        }
    }

    override fun isMutable(): Boolean {
        return values.size > 1
    }

    override fun copy(): Gene {
        //recall: "values" is immutable
        val copy = EnumGene<T>(name, values, index)
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        val k = if (forceNewValue) {
            randomness.nextInt(0, values.size - 1, index)
        } else {
            randomness.nextInt(0, values.size - 1)
        }

        index = k
    }

    override fun getValueAsPrintableString(): String {

        val res = values[index]
        if(res is String){
            return "\"$res\"";
        } else {
            return res.toString()
        }
    }

    override fun getValueAsRawString(): String {
        return values[index].toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.index = other.index
    }
}