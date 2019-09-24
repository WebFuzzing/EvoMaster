package org.evomaster.experiments.archiveMutation.stringProblem.allIndepStable

import com.google.inject.Inject
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.mutator.geneMutation.CharPool
import org.evomaster.core.search.service.Randomness
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class IndepStableStringProblemDefinition : StringProblemDefinition() {

    var optima: MutableList<String> = mutableListOf()

    override fun init(n : Int, sLength : Int, maxLength: Int){
        nTargets = n
        nGenes = n
        specifiedLength = sLength
        this.maxLength = maxLength
        optima.clear()
        (0 until nTargets).forEach { _ ->
            optima.add(randomString())
        }
    }

    override fun distance(individual: StringIndividual): Map<Int, Double> {
        return distance(individual.seeGenes().toMutableList())
    }

    fun distance(list: MutableList<StringGene>) : Map<Int, Double>{
        assert(list.size == optima.size)
        val result = mutableMapOf<Int, Double>()
        (0 until nTargets).forEach{ index ->
            result[index] = distance(index, list[index].value )
        }
        return result
    }

    private fun distance(index : Int, value : String) : Double{
        val target = optima[index]
        return leftDistance(target, value)
    }
}