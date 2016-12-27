package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness

/**
 * A gene that might or might not be active.
 * An example are for query parameters in URLs
 */
class OptionalGene(name: String,
                   val gene: Gene,
                   var isActive: Boolean = true)
    : Gene(name) {


    override fun copy(): Gene {
        return OptionalGene(name, gene.copy(), isActive)
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        if(! forceNewValue){
            isActive = randomness.nextBoolean()
            gene.randomize(randomness, false)
        } else {

            if(randomness.nextBoolean()){
                isActive = ! isActive
            } else {
                gene.randomize(randomness, true)
            }
        }
    }

    override fun getValueAsString() : String{
        return gene.getValueAsString()
    }
}