package org.evomaster.core.search.gene


class EnumGene<T>(
        name: String,
        val values: List<T>,
        var index: Int
)
    : Gene(name) {

    init {
        if (values.isEmpty()) {
            throw IllegalArgumentException("Empty list of values")
        }
        if (index < 0 || index >= values.size) {
            throw IllegalArgumentException("Invalid index: " + index)
        }
    }

    override fun isMutable() : Boolean{
        return values.size > 1
    }

    override fun copy() : Gene {
        //recall: "values" is immutable
        val copy = EnumGene<T>(name, values, index)
        return copy
    }
}