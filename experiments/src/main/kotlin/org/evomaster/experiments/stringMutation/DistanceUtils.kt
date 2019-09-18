package org.evomaster.experiments.stringMutation

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.ArchiveStringMutationUtils
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2019-09-16
 */
class DistanceUtils {

    companion object{

        fun distance(target : String, value: String) : Double{
            val distance = DistanceHelper.getLeftAlignmentDistance(target, value)
            return 1.0/(1.0 + distance)
        }

        fun exp(target: String, start: String) : Int{

            val gene = StringGene("stringGene",value = start, maxLength = if (target.length > 16) target.length else 16)

            val randomness = Randomness()

            var best = distance(target, gene.value)
            val improved = mutableListOf<StringGene>()
            improved.add(gene)

            var count = 0
            while (true) {
                count +=1
                val previous = improved.last()
                val mutated = previous.copy() as StringGene
                ArchiveStringMutationUtils.mutate(mutated, randomness, priorLengthMutation = true)
                val fitness = distance(target, mutated.value)
                val isBetter = fitness > best
                if (isBetter){
                    improved.add(mutated)
                    best = fitness
                }
                val previousValue = previous.value
                val currentValue = mutated.value
                ArchiveStringMutationUtils.evaluateMutation(previousValue, currentValue, mutated, improved.last(), isBetter, randomness = randomness)

                if (improved.last().targetLengthFoundAtMutatedIndex && improved.last().targetCharFoundAtMutatedIndex && improved.last().mutatedIndex >= improved.last().value.length)
                    break
            }

            assert(improved.last().value == target)

            return count
        }
    }
}


fun main(){
    val targets = listOf("1","r3KR0AImSMCIEka", "r3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaNr3KR0AImSMCIEkaN")//r3KR0AImSMCIEkaN
    val started = listOf("", "53n7B","r3KR0AImSMCIEka")//
    targets.forEach {target->
        val count = Array(started.size){0}
        started.forEachIndexed { index, start ->
            count[index] = DistanceUtils.exp(target, start)
        }
        val len = if (target.length > 16) target.length else 16

        println("========length: ${len}==========")
        println(count.map { "$it (${"%.2f".format(it/(len * 1.0)).toDouble() })" }.joinToString(", "))
    }

}


