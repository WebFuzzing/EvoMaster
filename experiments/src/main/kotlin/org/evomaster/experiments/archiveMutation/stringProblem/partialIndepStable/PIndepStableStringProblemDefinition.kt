package org.evomaster.experiments.archiveMutation.stringProblem.partialIndepStable

import com.google.inject.Inject
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.mutator.geneMutation.CharPool
import org.evomaster.core.search.service.Randomness
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemConfig
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition
import javax.annotation.PostConstruct

/**
 * created by manzh on 2019-09-16
 */
class PIndepStableStringProblemDefinition: StringProblemDefinition() {

    var optima: MutableList<String> = mutableListOf()

    @PostConstruct
    fun postConstruct() {
        charPool = sConfig.charPool
        rateOfImpact = sConfig.rateOfImpact
        val noImpact = if(sConfig.rateOfImpact == 1.0) 0 else (1/(1-sConfig.rateOfImpact)).toInt()
        nTargets = sConfig.numTarget
        nGenes = nTargets * (1 + noImpact) // only 1/3 genes contributes to fitness
        specifiedLength = sConfig.sLength
        this.maxLength = sConfig.maxLength
        generateOptima()
    }

    private fun generateOptima(){
        optima.clear()
        (0 until nTargets).forEach { _ ->
            optima.add(randomString())
        }
    }


    override fun distance(individual: StringIndividual): Map<Int, Double> {
        //if (optima.isEmpty()) generateOptima()
        return distance(individual.seeGenes().toMutableList())
    }

    private fun distance(list: MutableList<StringGene>) : Map<Int, Double>{
        assert(list.size * 3 == optima.size)
        val result = mutableMapOf<Int, Double>()
        (0 until nTargets).forEach{ index ->
            result[index] = leftDistance(optima[index], list[index * nGenes/nTargets].value )
        }
        return result
    }
}