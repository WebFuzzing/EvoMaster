package org.evomaster.experiments.archiveMutation.stringProblem.partialDepDynamic

import org.evomaster.core.search.gene.StringGene
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class PDepDynamicStringProblemDefinition : StringProblemDefinition() {

    override fun init(n : Int, sLength : Int, maxLength: Int){
        nTargets = n
        nGenes = n * 5 //only 2/5 genes contribute to fitness
        specifiedLength = sLength
        this.maxLength = maxLength
    }

    override fun distance(individual: StringIndividual): Map<Int, Double> {
        return distance(individual.genes)
    }

    private fun distance(list: MutableList<StringGene>) : Map<Int, Double>{
        if (list.size != nGenes)
            throw IllegalStateException("mismatched size of genes")

        return (0 until nTargets).map {
            Pair(it, leftDistance(list[it * 5].value, y(list[it*5 + 1].value)))
        }.toMap()
    }


}