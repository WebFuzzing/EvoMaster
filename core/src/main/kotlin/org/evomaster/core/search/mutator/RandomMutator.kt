package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene


class RandomMutator<T> : Mutator<T>() where T: Individual {

    override fun mutate(individual: T): T {

        val copy = individual.copy() as T

        val genes = copy.genes().filter(Gene::isMutable)

        var mutated = false

        while(! mutated) { //no point in returning a copy that is not mutated

            for (gene in genes) {

                val p = 1.0 / genes.size

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                var k: Int
                if (gene is IntegerGene) {
                    k = randomness.nextInt(gene.min, gene.max, gene.value)
                    gene.value = k
                } else if (gene is EnumGene<*>) {
                    k = randomness.nextInt(0, gene.values.size - 1, gene.index)
                    gene.index = k
                } else {
                    throw IllegalStateException("Unrecognized type: " + gene.javaClass)
                }
                mutated = true
            }
        }

        return copy
    }

}