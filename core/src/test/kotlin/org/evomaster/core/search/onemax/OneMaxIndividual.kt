package org.evomaster.core.search.onemax

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene


class OneMaxIndividual(val n: Int) : Individual() {

    private val list : MutableList<EnumGene<Double>> = mutableListOf()

    init {
        (0 until n).forEach {
            list.add(EnumGene<Double>("$it", listOf(0.0, 0.5, 1.0), 0))
        }
    }

    override fun copy(): Individual {

        var copy = OneMaxIndividual(n)
        (0 until n).forEach {
            copy.list[it].index = this.list[it].index
        }
        return copy
    }


    fun getValue(index: Int) : Double {
        val gene = list[index]
        return gene.values[gene.index]
    }

    fun setValue(index: Int, value: Double){
        val gene = list[index]
        when(value){
            0.0 -> gene.index = 0
            0.5 -> gene.index = 1
            1.0 -> gene.index = 2
        }

    }

    override fun genes(): List<out Gene> {
        return list
    }

    override fun size() : Int {
        return n
    }
}