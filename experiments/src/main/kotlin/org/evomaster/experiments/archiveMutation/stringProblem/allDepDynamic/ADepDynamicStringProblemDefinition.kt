package org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic

import org.evomaster.core.search.gene.StringGene
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class ADepDynamicStringProblemDefinition : StringProblemDefinition() {

    override fun init(n : Int, sLength : Int, maxLength: Int ){
        nTargets = n
        nGenes = n * 2
        specifiedLength = sLength
        this.maxLength = maxLength
    }

    override fun distance(individual: StringIndividual): Map<Int, Double> {
        return distance(individual.genes)
    }

    private fun distance(list: MutableList<StringGene>) : Map<Int, Double>{
        if (list.size != nGenes)
            throw IllegalStateException("mismatched size of genes")

        return (0 until nTargets).map { Pair(it, leftDistance(list[it].value, y(list[it+1].value))) }.toMap()
    }

}