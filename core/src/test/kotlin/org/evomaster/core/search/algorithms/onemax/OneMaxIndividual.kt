package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.tracer.TraceableElement


class OneMaxIndividual : Individual {
    val n: Int
    private val list : MutableList<EnumGene<Double>> = mutableListOf()

    constructor(n : Int) : super(){
        this.n = n
        init()
    }

    constructor(n : Int, description: String) : super(description){
        this.n = n
        init()
    }

    constructor(n : Int, description: String, traces : MutableList<OneMaxIndividual>) : super(description,traces){
        this.n = n
        init()
    }

    override fun isCapableOfTracking(): Boolean = true

    override fun next(description: String): TraceableElement? {
        if(isCapableOfTracking()){
            val copyTraces = mutableListOf<OneMaxIndividual>()
            if(!isRoot()){
                val size = getTrack()?.size?:0
                (0 until if(maxlength!= -1 && size > maxlength - 1) maxlength-1  else size).forEach {
                    copyTraces.add(0, (getTrack()!![size-1-it] as OneMaxIndividual).copy() as OneMaxIndividual)
                }
            }
            copyTraces.add(this.copy() as OneMaxIndividual)
            return OneMaxIndividual(
                    n,
                    description,
                    copyTraces)
        }
        return copy()
    }

    override fun copy(withTrack: Boolean): TraceableElement {
        when(withTrack){
            false-> return copy()
            else ->{
                getTrack()?:return copy()
                val copyTraces = mutableListOf<OneMaxIndividual>()
                getTrack()?.forEach {
                    copyTraces.add((it as OneMaxIndividual).copy() as OneMaxIndividual)
                }
                return OneMaxIndividual(
                        n,
                        getDescription(),
                        copyTraces)
            }
        }
    }


    private fun init() {
        (0 until n).forEach {
            list.add(EnumGene<Double>("$it", listOf(0.0, 0.25, 0.5, 0.75, 1.0), 0))
        }
    }

    override fun copy(): Individual {

        var copy = OneMaxIndividual(n, getDescription())
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
            0.0  -> gene.index = 0
            0.25 -> gene.index = 1
            0.5  -> gene.index = 2
            0.75 -> gene.index = 3
            1.0  -> gene.index = 4
            else -> throw IllegalArgumentException("Invalid value $value")
        }
    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return list
    }

    override fun size() : Int {
        return n
    }

    override fun seeActions(): List<out Action> {
        return listOf()
    }

    override fun verifyInitializationActions(): Boolean {
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {

    }
}