package org.evomaster.core.search.onemax

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene


class OneMaxIndividual(val n: Int) : Individual() {

    private var array : DoubleArray

    init {
        array = DoubleArray(n)
    }

    override fun copy(): Individual {

        var copy = OneMaxIndividual(n)
        copy.array = this.array.copyOf()
        return copy
    }

    fun getValue(index: Int) : Double = array[index]

    fun setValue(index: Int, value: Double){
        array[index] = value
    }

    override fun genes(): List<out Gene> {

        return listOf()
    }

    override fun size() : Int {
        return n
    }
}