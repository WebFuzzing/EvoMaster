package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Mutator


class RandomMutator<T> : Mutator<T>() where T: Individual {

    override fun mutate(individual: T): T {

        val copy = individual.copy() as T

        val genes = copy.seeGenes().filter(Gene::isMutable)

        if(genes.isEmpty()){
            return copy
        }

        /*
            Probability 1/n that a gene is going to be mutated.
            On average, 1 mutation per individual.
            However, non-null probability of no modifications
         */
        val p = 1.0 / genes.size

        var mutated = false

        while(! mutated) { //no point in returning a copy that is not mutated

            for (gene in genes) {

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                gene.randomize(randomness, true)

                mutated = true
            }
        }

        return copy
    }

}