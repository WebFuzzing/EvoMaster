package org.evomaster.core.search.gene


class IntegerGene(
        name: String,
        /** Inclusive */
        val min: Int,
        /** Inclusive */
        val max: Int,
        var value: Int
) : Gene(name){


    override fun copy() : Gene{
        val copy = IntegerGene(name, min, max, value)
        return copy
    }
}