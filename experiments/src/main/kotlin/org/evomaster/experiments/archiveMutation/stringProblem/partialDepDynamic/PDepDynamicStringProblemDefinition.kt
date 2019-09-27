package org.evomaster.experiments.archiveMutation.stringProblem.partialDepDynamic

import com.google.inject.Inject
import org.evomaster.core.search.gene.StringGene
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition
import javax.annotation.PostConstruct

/**
 * created by manzh on 2019-09-16
 */
class PDepDynamicStringProblemDefinition: StringProblemDefinition(){

    @PostConstruct
    fun postConstruct() {
        charPool = sConfig.charPool
        rateOfImpact = sConfig.rateOfImpact
        val noImpact = if(sConfig.rateOfImpact == 1.0) 0 else (1/(1-sConfig.rateOfImpact)).toInt()
        nTargets = sConfig.numTarget
        nGenes = sConfig.numTarget * (2 + noImpact) //only 2/5 genes contribute to fitness
        specifiedLength = sConfig.sLength
        this.maxLength = sConfig.maxLength
    }

    override fun distance(individual: StringIndividual): Map<Int, Double> {
        return distance(individual.genes)
    }

    private fun distance(list: MutableList<StringGene>) : Map<Int, Double>{
        if (list.size != nGenes)
            throw IllegalStateException("mismatched size of genes")

        return (0 until nTargets).map {
            Pair(it, leftDistance(list[it * nGenes/nTargets].value, y(list[it * nGenes/nTargets + 1].value)))
        }.toMap()
    }


}