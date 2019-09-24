package org.evomaster.experiments.archiveMutation.stringProblem

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator

/**
 * created by manzh on 2019-09-16
 */
class StringIndividual (val genes : MutableList<StringGene>,
                        trackOperator: TrackOperator? = null,
                        traces : MutableList<StringIndividual>? = null)
    : Individual (trackOperator,traces) {

    constructor(n : Int, trackOperator: TrackOperator?, traces: MutableList<StringIndividual>?) : this((0 until n).map { StringGene(it.toString()) }.toMutableList(), trackOperator, traces)

    override fun verifyInitializationActions(): Boolean {
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {
    }

    override fun seeGenes(filter: GeneFilter): List<StringGene> {
        return genes
    }

    override fun size(): Int {
        return genes.size
    }

    override fun seeActions(): List<out Action> = listOf()

    override fun copy(): Individual {
        return StringIndividual(genes.map { it.copy() as StringGene }.toMutableList())
    }

    override fun next(trackOperator: TrackOperator, maxLength : Int): TraceableElement? {
        getTracking()?: return StringIndividual(genes.map { it.copy() as StringGene }.toMutableList(), trackOperator)
        return StringIndividual(
                genes.map { it.copy() as StringGene }.toMutableList(),
                trackOperator,
                getTracking()!!.run {
                    if (size == maxLength)
                        this.subList(1, this.size)
                    else
                        this
                }.plus(this).map { (it as StringIndividual).copy() as StringIndividual }.toMutableList()
        )
    }

    override fun copy(copyFilter: TraceableElementCopyFilter): TraceableElement {
        when(copyFilter){
            TraceableElementCopyFilter.NONE-> return copy()
            TraceableElementCopyFilter.WITH_TRACK, TraceableElementCopyFilter.DEEP_TRACK ->{
                getTracking()?:return copy()
                return StringIndividual(
                        genes.map { it.copy() as StringGene }.toMutableList(),
                        trackOperator,
                        getTracking()!!.map { (it as StringIndividual).copy() as StringIndividual }.toMutableList()
                )
            }else -> throw IllegalStateException("${copyFilter.name} is not implemented!")
        }
    }
}