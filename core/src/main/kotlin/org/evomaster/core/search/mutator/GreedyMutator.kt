package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Mutator

/*
    TODO variant of AVM, see recent McMinn and code at:

    https://github.com/AVMf/avmf/blob/master/src/main/java/org/avmframework/localsearch/LatticeSearch.java
    http://mcminn.io/publications/j17.pdf
 */
class GreedyMutator <T> : Mutator<T>() where T: Individual {

    override fun mutate(individual: T): T {
        val copy = individual.copy() as T

        val genes = copy.seeGenes().filter(Gene::isMutable)

        if(genes.isEmpty()){
            return copy
        }

        var mutated = false

        while(! mutated) { //no point in returning a copy that is not mutated

            for (gene in genes) {

                val p = 1.0 / genes.size

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                mutateGene(gene)

                mutated = true
            }
        }

        return copy
    }

    private fun mutateGene(gene: Gene){

        if(gene is IntegerGene){

            assert(gene.min < gene.max && gene.isMutable())

            when(gene.value){
                gene.max -> gene.value--
                gene.min -> gene.value++
                else -> gene.value + randomness.choose(listOf(-1, +1))
            }
        } else {
            //TODO other cases
            gene.randomize(randomness, true)
        }
    }
}