package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.service.Mutator


class StandardMutator<T> : Mutator<T>() where T : Individual {

    private val intpow2 = (0..30).map { Math.pow(2.0, it.toDouble()).toInt() }


    override fun mutate(individual: T): T {
        val copy = individual.copy() as T

        if(individual.canMutateStructure() && randomness.nextBoolean()){
            //usually, either delete an action, or add a new random one
            structureMutator.mutateStructure(copy)
            return copy
        }

        val genes = copy.seeGenes().filter(Gene::isMutable)

        if (genes.isEmpty()) {
            return copy
        }

        val p = 1.0 / genes.size

        var mutated = false

        while (!mutated) { //no point in returning a copy that is not mutated

            for (gene in genes) {

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                if(gene is DisruptiveGene<*> && ! randomness.nextBoolean(gene.probability)){
                   continue
                }

                mutateGene(gene)

                mutated = true
            }
        }

        return copy
    }

    private fun mutateGene(gene: Gene) {

        when (gene) {
            is DisruptiveGene<*> -> mutateGene(gene.gene)
            is IntegerGene -> handleIntegerGene(gene)
            is OptionalGene -> handleOptionalGene(gene)
            else ->
                //TODO other cases
                gene.randomize(randomness, true)
        }
    }


    private fun handleOptionalGene(gene: OptionalGene){
        if(! gene.isActive){
            gene.isActive = true
        } else {

            if(randomness.nextBoolean(0.01)){
                gene.isActive = false
            } else {
                mutateGene(gene.gene)
            }
        }
    }

    private fun handleIntegerGene(gene: IntegerGene) {
        assert(gene.min < gene.max && gene.isMutable())

        //check maximum range. no point in having a delta greater than such range
        val range: Long = gene.max.toLong() - gene.min.toLong()

        val maxIndex = apc.getExploratoryValue(intpow2.size, 10)

        var n = 0
        for (i in 0 until maxIndex) {
            n = i + 1
            if (intpow2[i] > range) {
                break
            }
        }

        //choose an i for 2^i modification
        val delta = randomness.chooseUpTo(intpow2, n)
        val sign = when (gene.value) {
            gene.max -> -1
            gene.min -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        val res: Long = (gene.value.toLong()) + (sign * delta)

        gene.value = when {
            res > gene.max -> gene.max
            res < gene.min -> gene.min
            else ->  res.toInt()
        }
    }
}