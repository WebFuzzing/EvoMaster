package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import kotlin.math.pow

/**
 * created by manzh on 2019-09-16
 */
class ArchiveStringMutationUtils {

    companion object{

        fun mutate(gene : StringGene, randomness: Randomness, probOfModifyChar : Double = 0.5, priorLengthMutation : Boolean = true){
            if (gene.value.isEmpty() || gene.mutatedIndex == gene.value.length){
                append(gene, randomness)
                return
            }

            if (priorLengthMutation){
                if (!gene.targetLengthFoundAtMutatedIndex){
                    modifyLength(gene, randomness)
                    return
                }
            }

            if (gene.targetLengthFoundAtMutatedIndex){
                modify(gene, randomness, middle = 0.5)
                return
            }

            if (gene.targetCharFoundAtMutatedIndex && !gene.targetLengthFoundAtMutatedIndex){
                when(randomness.nextBoolean(probOfModifyChar)){
                    true-> modify(gene, randomness, middle = 0.5)
                    false-> modifyLength(gene, randomness)
                }
            }else{
                modify(gene, randomness, middle = 0.5)
            }

        }

        private fun modifyLength(gene: StringGene, randomness: Randomness){
            val current = gene.value.length
            val modifiedIndex = gene.mutatedIndex
            val min = if (gene.mutatedIndex > gene.preferLengthMin && modifiedIndex <= gene.preferLengthMax) gene.mutatedIndex else gene.preferLengthMin
            val validCandidates = (min..gene.preferLengthMax).toMutableList()
            validCandidates.remove(current)

            if (validCandidates.isEmpty()){
                //remove
                delete(gene)
                return
            }

            val selected = if (validCandidates.size == 1) {
              validCandidates.first()
            }else {
                //randomness.choose(validCandidates)
                val valve = (gene.maxLength - gene.minLength).toDouble().pow(1.0 / 2).toInt() - 1
                random(false, gene.noImproveLengthCounter > (if (valve < 2) 2 else valve), current, 0.5, validCandidates, randomness)

            }
            if (selected < current){
                //remove
                delete(gene, current - selected)
            }else{
                //append
                append(gene, randomness, num = selected - current)
            }

        }

        private fun modify(gene : StringGene,randomness: Randomness, charPool: CharPool = CharPool.WORD, middle : Double) {
            if (gene.targetCharFoundAtMutatedIndex){
                gene.targetCharFoundAtMutatedIndex = false
            }else if (gene.mutatedIndex == -1){
                gene.mutatedIndex += 1
                resetChar(gene, randomness, charPool)
            }
            val current = gene.value.toCharArray()[gene.mutatedIndex].toInt()
            val validCandidates = validateCandidates(gene.preferCharMin, gene.preferCharMax, randomness, charPool, current)
            if (validCandidates.isEmpty())
                throw IllegalArgumentException("bug")
            if (validCandidates.size == 1){
                gene.value = modifyIndex(gene.value, gene.mutatedIndex, validCandidates.first().toChar())
            }else{
//                val char = if (gene.noImproveCharCounter > 3){
//                    findClosest(current, validCandidates, randomness).toChar()
//                }else{
//                    when(randomness.nextBoolean(middle)){
//                        true -> validCandidates[validCandidates.size/2].toChar()
//                        false -> randomness.choose(validCandidates).toChar()
//                    }
//                }
                val char = random(false, gene.noImproveCharCounter > 3, current, middle, validCandidates, randomness).toChar()

                gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
            }
        }

        private fun random(enableCounter : Boolean, exceed : Boolean, current: Int, probOfMiddle : Double, validCandidates: List<Int>, randomness: Randomness) : Int{
            if (enableCounter){
                if (exceed)
                    return findClosest(current, validCandidates, randomness)
            }

            return when(randomness.nextBoolean(probOfMiddle)){
                true -> validCandidates[validCandidates.size/2]
                false -> randomness.choose(validCandidates)
            }
        }

        private fun findClosest(current: Int, validates : List<Int>, randomness: Randomness) : Int{
            val sorted = validates.plus(current).toHashSet().sorted()
            val index = sorted.indexOf(current)
            if (index == 0)
                return sorted[1]
            if (index == sorted.size - 1)
                return sorted[index - 1]
            if (randomness.nextBoolean())
                return sorted[index-1]
            return sorted[index+1]

        }

        private fun delete(gene : StringGene, num: Int = 1){
            gene.value = gene.value.substring(0, gene.value.length - num)
        }

        private fun append(gene : StringGene,randomness: Randomness, charPool: CharPool = CharPool.WORD, num : Int = 1){
            (0 until num).forEach { _ ->
                gene.value += when(charPool){
                    CharPool.WORD -> randomness.nextWordChar()
                    CharPool.ALL -> randomness.nextChar(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())
                    else ->  TODO("NOT IMPLEMENT")
                }
            }

        }

        private fun modifyIndex(value : String, index : Int, char: Char) : String{
            if (index >= value.length) throw IllegalArgumentException("$index exceeds the length of $value")
            return value.toCharArray().also {it[index] = char }.joinToString("")
        }

        fun evaluateMutation(previous: String, current: String, mutated:StringGene, savedGene: StringGene, doesCurrentBetter: Boolean, charPool: CharPool = CharPool.WORD, randomness: Randomness){
            if (savedGene != mutated){
                savedGene.copyMutationInfo(mutated)
            }
            if (previous.length != current.length){
                val boundary = Pair(savedGene.preferLengthMin, savedGene.preferLengthMax)
                val update = updateBoundary(boundary, previous.length, current.length, doesCurrentBetter)
                savedGene.preferLengthMin = update.first
                savedGene.preferLengthMax = update.second

                if (!doesCurrentBetter)
                    savedGene.noImproveLengthCounter += 1
                else
                    savedGene.noImproveLengthCounter = 0

                if (update.first == update.second){
                    savedGene.targetLengthFoundAtMutatedIndex = true
                    if (savedGene.value.isEmpty()){
                        savedGene.targetCharFoundAtMutatedIndex = true
                        savedGene.mutatedIndex = 0
                    }
                }
            }else{
                val boundary = Pair(savedGene.preferCharMin, savedGene.preferCharMax)
                assert(savedGene.mutatedIndex != -1)
                val update = updateBoundary(boundary, previous[savedGene.mutatedIndex].toInt(), current[savedGene.mutatedIndex].toInt(), doesCurrentBetter)
                savedGene.preferCharMin = update.first
                savedGene.preferCharMax = update.second
                val exclude = savedGene.value[savedGene.mutatedIndex].toInt()

                if (!doesCurrentBetter)
                    savedGene.noImproveCharCounter += 1
                else
                    savedGene.noImproveCharCounter = 0

                if (!checkIfHasCandidates(savedGene.preferCharMin, savedGene.preferCharMax, randomness, charPool = charPool, exclude = exclude)){
                    savedGene.targetCharFoundAtMutatedIndex = true
                    savedGene.mutatedIndex += 1
                    savedGene.noImproveCharCounter = 0
                    resetChar(savedGene, randomness, charPool)
                }
            }
        }

        private fun resetChar(gene: StringGene, randomness: Randomness, charPool: CharPool){
            when(charPool){
                CharPool.WORD -> {
                    gene.preferCharMin = randomness.wordCharPool().first()
                    gene.preferCharMax = randomness.wordCharPool().last()
                }
                CharPool.ALL -> {
                    gene.preferCharMin = Char.MIN_VALUE.toInt()
                    gene.preferCharMax = Char.MAX_VALUE.toInt()
                }
                else ->  TODO("NOT IMPLEMENT")
            }
        }

        private fun validateCandidates(min : Int, max:Int, randomness: Randomness, charPool: CharPool, exclude : Int) : MutableList<Int>{
            val validCandidates = when(charPool){
                CharPool.WORD -> randomness.validNextWordChars(min, max).toMutableList()
                CharPool.ALL -> (min..max).toMutableList()
                else ->  TODO("NOT IMPLEMENT")
            }
            validCandidates.remove(exclude)
            return validCandidates
        }

        private fun checkIfHasCandidates(min : Int, max:Int, randomness: Randomness, charPool: CharPool, exclude : Int) = validateCandidates(min, max, randomness, charPool, exclude).isNotEmpty()

        private fun updateBoundary(boundary : Pair<Int, Int>, previous : Int, current: Int, doesCurrentBetter : Boolean) : Pair<Int, Int>{
            val value = (previous + current) / 2.0

            return if ( (doesCurrentBetter && current > previous) || (!doesCurrentBetter && current < previous)){
                Pair(if (value > value.toInt()) value.toInt()+1 else value.toInt(), boundary.second)
            }else
                Pair(boundary.first, value.toInt())

        }
    }
}

enum class CharPool{
    ALL,
    WORD,
    LETTER
}