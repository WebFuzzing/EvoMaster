package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator


class OneMaxIndividual(
        val n : Int,
        trackOperator: TrackOperator? = null,
        index : Int = -1,
        action: OneMaxAction = createGenes(n)

): Individual (trackOperator, index, mutableListOf(action),{ k -> OneMaxAction::class.java.isAssignableFrom(k)}) {


    companion object {
        fun createGenes(n: Int): OneMaxAction {
            val list = mutableListOf<EnumGene<Double>>()
            (0 until n).forEach {
                val gene = EnumGene<Double>("$it", listOf(0.0, 0.25, 0.5, 0.75, 1.0), 0)
                list.add(gene)
            }

            return OneMaxAction(list)
        }
    }

    private fun getAction() = children[0] as OneMaxAction

//    fun initialize(rand: Randomness? = null){
//        children.forEach{a -> (a as Action).doInitialize(rand)}
//    }

    fun resetAllToZero(){
        (0 until getAction().list.size).forEach {
            setValue(it, 0.0)
        }
    }

    override fun copyContent(): Individual {
        return OneMaxIndividual(n, trackOperator, index, children[0].copy() as OneMaxAction)
    }


    fun getValue(index: Int) : Double {
        val gene = getAction().list[index]
        return gene.values[gene.index]
    }

    fun setValue(index: Int, value: Double){
        val gene = getAction().list[index]
        when(value){
            0.0  -> gene.index = 0
            0.25 -> gene.index = 1
            0.5  -> gene.index = 2
            0.75 -> gene.index = 3
            1.0  -> gene.index = 4
            else -> throw IllegalArgumentException("Invalid value $value")
        }
    }

    override fun seeTopGenes(filter: ActionFilter): List<Gene> {
        return getAction().list
    }

    override fun size() : Int {
        /*
            there is only 1 single action in the individual.
            in the past, we used N as size, but that screw up a lot of assumptions in the current
            behavior of the search.
            As OneMax is only used in the tests, we fix it, although many unit tests might still refer
            to the old behaviour
         */
        return 1
    }


    override fun verifyInitializationActions(): Boolean {
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {

    }
}