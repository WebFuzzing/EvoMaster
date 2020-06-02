package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
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
    lateinit var archiveMutator : ArchiveMutator


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
            forceNotEmpty : Boolean = true) : List<Gene>{

        val numToMutate = apc.getExploratoryValue(max(1.0, config.startingPerOfGenesToMutate * candidateGenesToMutate.size), 1.0)
        val mutated = mutableListOf<Gene>()

        //by default, weight of all mutable genes is 1.0
        val weights = candidateGenesToMutate.map { Pair(it, 1.0) }.toMap().toMutableMap()

        /*
            mutation rate can be manipulated by different weight methods
            eg, only depends on static weight, or impact derived based on archive (archive-based solution)
         */
        if(adaptiveWeight){
            if (targets == null || (individual == null && impacts == null) || evi == null)
                throw IllegalArgumentException("invalid inputs: when adaptive weight is applied, targets, evi and individual(or impacts) should not be null")
            archiveMutator.calculateWeightByArchive(candidateGenesToMutate, weights, individual = individual, impacts = impacts, evi = evi, targets = targets)
        } else{
            candidateGenesToMutate.forEach {
                weights[it] = it.mutationWeight().toDouble()
            }
        }

        do {
            val sw = weights.values.sum()
            candidateGenesToMutate.forEach { g->
                val m = calculatedAdaptiveMutationRate(candidateGenesToMutate.size, config.d, numToMutate, sw, weights.getValue(g))
                if (randomness.nextBoolean(m))
                    mutated.add(g)
            }
        }while (forceNotEmpty && mutated.isEmpty())

        return mutated
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