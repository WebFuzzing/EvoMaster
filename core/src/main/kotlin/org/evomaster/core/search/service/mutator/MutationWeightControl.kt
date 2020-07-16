package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneSelector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

/**
 * control mutation weight which requires [apc] and [archiveMutator]
 */
class MutationWeightControl {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config : EMConfig

    @Inject
    lateinit var apc : AdaptiveParameterControl

    @Inject
    lateinit var archiveMutator : ArchiveGeneSelector

    companion object{
        private val log: Logger = LoggerFactory.getLogger(MutationWeightControl::class.java)
    }

    /**
     * @return a subset of [candidateGenesToMutate] to mutate with weight-based solution.
     */
    fun selectSubGene(
            candidateGenesToMutate: List<Gene>,
            adaptiveWeight: Boolean,
            targets: Set<Int>? = null,
            impacts: List<Impact>?= null,
            individual: Individual?= null,
            evi: EvaluatedIndividual<*>?= null,
            forceNotEmpty : Boolean = true,
            numOfGroup: Int = 1) : List<Gene>{

        if (candidateGenesToMutate.isEmpty()){
            if (forceNotEmpty)
                throw IllegalArgumentException("candidate is empty")
            else {
                return emptyList()
            }
        }
        if (candidateGenesToMutate.size == 1 && forceNotEmpty)
            return candidateGenesToMutate

        val numToMutate = apc.getExploratoryValue(max(1.0, config.startingPerOfGenesToMutate * candidateGenesToMutate.size), 1.0/numOfGroup)
        val mutated = mutableListOf<Gene>()

        //by default, weight of all mutable genes is 1.0
        val weights = candidateGenesToMutate.map { Pair(it, 1.0) }.toMap().toMutableMap()

        /*
            mutation rate can be manipulated by different weight methods
            eg, only depends on static weight, or impact derived based on archive (archive-based solution)
         */
        if(adaptiveWeight){
            if (targets == null)
                throw IllegalArgumentException("invalid inputs: when adaptive weight is applied, targets should not be null")
            else if(evi != null && individual != null)
                archiveMutator.calculateWeightByArchive(candidateGenesToMutate, weights, individual = individual, evi = evi, targets = targets)
            else if (impacts != null)
                archiveMutator.calculateWeightByArchive(candidateGenesToMutate, weights, impacts, targets = targets)
            else
                throw IllegalArgumentException("invalid inputs: when adaptive weight is applied, individual and evaluated individual (or impacts) should not be null")
        } else{
            candidateGenesToMutate.forEach {
                weights[it] = it.mutationWeight().toDouble()
            }
        }

        Lazy.assert {
            weights.size == candidateGenesToMutate.size
        }

        mutated.addAll(selectSubsetWithWeight(weights, forceNotEmpty, numToMutate))
        if (mutated.isEmpty() && forceNotEmpty) {
           throw IllegalStateException("with ${candidateGenesToMutate.size} candidates, none to be selected")
        }

        return mutated
    }

    fun <T>selectSubsetWithWeight(weights : Map<T, Double>, forceNotEmpty: Boolean, numToMutate : Double) : List<T>{
        if (weights.isEmpty()) throw IllegalArgumentException("Cannot select with an empty list")
        if (weights.size == 1) return weights.keys.toList()
        val results  = mutableListOf<T>()
        do {
            val sw = weights.values.sum()
            val size = weights.size
            weights.keys.forEach { g->
                val m = calculatedAdaptiveMutationRate(size, config.d, numToMutate, sw, weights.getValue(g))
                if (randomness.nextBoolean(m))
                    results.add(g)
            }
        }while (forceNotEmpty && results.isEmpty())

        return results
    }

    /**
     * @return mutation rate for a given gene with
     * @param n a total number of candidate genes to mutate
     * @param d a tunable parameter [0,1]
     * @param t average number of genes to mutate
     * @param w a weight of the given gene
     * @param sw a sum of weights of all of the candidates genes
     */
    private fun calculatedAdaptiveMutationRate(n : Int, d : Double, t: Double, sw: Double, w : Double) = t * (d/n + (1.0 - d) * w/sw)

}