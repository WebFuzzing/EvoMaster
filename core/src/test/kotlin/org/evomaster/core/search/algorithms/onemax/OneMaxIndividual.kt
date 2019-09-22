package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator


class OneMaxIndividual(
        val n : Int,
        trackOperator: TrackOperator? = null,
        traces : MutableList<OneMaxIndividual>? = null)
    : Individual (trackOperator,traces) {

    private val list : MutableList<EnumGene<Double>> = mutableListOf()

    init {
        (0 until n).forEach {
            list.add(EnumGene<Double>("$it", listOf(0.0, 0.25, 0.5, 0.75, 1.0), 0))
        }
    }

    override fun next(trackOperator: TrackOperator, maxLength : Int): TraceableElement? {
        getTracking()?: return OneMaxIndividual(n, trackOperator)
        return OneMaxIndividual(
                n,
                trackOperator,
                (getTracking()!!.plus(this).map { (it as OneMaxIndividual).copy() as OneMaxIndividual }.toMutableList()).run {
                    if (size == maxLength){
                        this.removeAt(0)
                        this
                    }else
                        this
                }
        )
    }

    override fun copy(copyFilter: TraceableElementCopyFilter): TraceableElement {
        when(copyFilter){
            TraceableElementCopyFilter.NONE-> return copy()
            TraceableElementCopyFilter.WITH_TRACK, TraceableElementCopyFilter.DEEP_TRACK ->{
                getTracking()?:return copy()
                return OneMaxIndividual(
                        n,
                        trackOperator,
                        getTracking()!!.map { (it as OneMaxIndividual).copy() as OneMaxIndividual }.toMutableList()
                )
            }else -> throw IllegalStateException("${copyFilter.name} is not implemented!")
        }
    }


    private fun init() {
        (0 until n).forEach {
            list.add(EnumGene<Double>("$it", listOf(0.0, 0.25, 0.5, 0.75, 1.0), 0))
        }
    }

    override fun copy(): Individual {

        var copy = OneMaxIndividual(n, trackOperator)
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