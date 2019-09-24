package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.ImpactUtils

/**
 * created by manzh on 2019-09-10
 *
 * @property mutatedGenes records what genes are mutated
 * @property addedGenes records what genes are added using structure mutator
 * @property removedGene records what genes are removed using structure mutator
 * @property mutatedPosition records where mutated/added/removed genes are located. but regarding different individual,
 * the position may be parsed in different way. For instance, the position may indicate the position of resource calls,
 * not rest action.
 */
open class MutatedGeneSpecification (
        val mutatedGenes : MutableList<Gene> = mutableListOf(),
        val addedGenes : MutableList<Gene> = mutableListOf(),
        val removedGene: MutableList<Gene> = mutableListOf(),
        val mutatedPosition : MutableList<Int> = mutableListOf()
){
    var mutatedIndividual: Individual? = null
        private set

    fun setMutatedIndividual(individual: Individual){
        if (mutatedIndividual!= null)
            throw IllegalArgumentException("it does not allow setting mutated individual more than one time")
        mutatedIndividual = individual
    }

    fun copyFrom(current: EvaluatedIndividual<*>) : MutatedGeneSpecification{
        val spec = MutatedGeneSpecification()
        mutatedGenes.forEach { s->
            val id = ImpactUtils.generateGeneId(mutatedIndividual!!, s)
            val savedGene = (current.findGeneById(id) ?: throw IllegalStateException("mismatched genes"))
            spec.mutatedGenes.add(savedGene)
        }
        addedGenes.forEach { s->
            val id = ImpactUtils.generateGeneId(mutatedIndividual!!, s)
            val savedGene = (current.findGeneById(id) ?: throw IllegalStateException("mismatched genes"))
            spec.addedGenes.add(savedGene)
        }
        spec.mutatedPosition.addAll(mutatedPosition.toMutableList())
        spec.setMutatedIndividual(current.individual)
        return spec
    }
}