package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.Mutator
import kotlin.math.min

/**
 * created by manzh on 2020-06-24
 */
class ManipulatedOneMaxMutator : Mutator<OneMaxIndividual>() {

    var improve = false

    override fun mutate(individual: EvaluatedIndividual<OneMaxIndividual>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?): OneMaxIndividual {
        return manipulate(individual, 0.25, improve = improve, mutatedGeneSpecification = mutatedGenes)
    }

    override fun genesToMutation(individual: OneMaxIndividual, evi: EvaluatedIndividual<OneMaxIndividual>, targets: Set<Int>): List<Gene> {
        return individual.seeTopGenes(ActionFilter.ALL).filter { it.isMutable() }
    }

    override fun selectGenesToMutate(individual: OneMaxIndividual, evi: EvaluatedIndividual<OneMaxIndividual>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?): List<Gene> {
       return listOf()
    }

    override fun doesStructureMutation(evaluatedIndividual: EvaluatedIndividual<OneMaxIndividual>): Boolean = false

    private fun manipulate(mutated: EvaluatedIndividual<OneMaxIndividual>, degree: Double, improve: Boolean, mutatedGeneSpecification: MutatedGeneSpecification?) : OneMaxIndividual{
        val ind = mutated.individual.copy() as OneMaxIndividual


        val index = if(improve)(0 until ind.n).firstOrNull{ind.getValue(it)  < 1.0} else (0 until ind.n).filter{ind.getValue(it)  >= 0.25}.run { if (isEmpty()) null else randomness.choose(this)}
        index?:return ind

        val previousValue = ind.getValue(index)
        ind.setValue(index, if(improve) min(1.0, ind.getValue(index) + degree) else min(0.0, ind.getValue(index) - degree))

        mutatedGeneSpecification?.addMutatedGene(isDb = false, isInit = false, valueBeforeMutation = previousValue.toString(), gene = ind.seeTopGenes()[index], localId = null, position = 0)

        return ind
    }
}