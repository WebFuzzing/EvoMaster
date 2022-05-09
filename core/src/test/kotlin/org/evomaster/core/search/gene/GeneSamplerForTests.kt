package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import kotlin.reflect.KClass

object GeneSamplerForTests {

     fun <T> sample(klass :KClass<T>, rand: Randomness) : T where T : Gene{

         return when (klass) {
             StringGene::class -> sampleStringGene(rand) as T
             else -> throw IllegalStateException("No sampler for $klass")

             //TODO need for all Genes
             // when genes need input genes, we sample those at random as well
         }
     }

    fun sampleStringGene(rand: Randomness) : StringGene{

        val min = rand.nextInt(0,3)
        val max = min + rand.nextInt(20)

        return StringGene("rand string ${rand.nextInt()}", minLength=min, maxLength=max)
    }

}