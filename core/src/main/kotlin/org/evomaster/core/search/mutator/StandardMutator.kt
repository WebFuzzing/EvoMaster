package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Mutator


class StandardMutator<T> : Mutator<T>() where T: Individual {

    private val intpow2 = (0..30).map{ Math.pow(2.0, it.toDouble()).toInt()}


    override fun mutate(individual: T): T {
        val copy = individual.copy() as T

        val genes = copy.seeGenes().filter(Gene::isMutable)

        if(genes.isEmpty()){
            return copy
        }

        val p = 1.0 / genes.size

        var mutated = false

        while(! mutated) { //no point in returning a copy that is not mutated

            for (gene in genes) {

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

            //check maximum range. no point in having a delta greater than such range
            val range: Long = gene.max.toLong() - gene.min.toLong()
            var n = 0
            for(i in 0 until  intpow2.size){
                n = i
                if(intpow2[i] > range){
                    break
                }
            }

            //choose an i for 2^i modification
            val delta = randomness.chooseUpTo(intpow2,n)
            val sign = when(gene.value){
                gene.max -> -1
                gene.min -> +1
                else -> randomness.choose(listOf(-1, +1))
            }

            val res : Long =  (gene.value.toLong()) + (sign * delta)

            gene.value = when{
                res > gene.max ->  gene.max
                res < gene.min ->  gene.min
                else -> res.toInt()
            }
        } else {
            //TODO other cases
            gene.randomize(randomness, true)
        }
    }
}