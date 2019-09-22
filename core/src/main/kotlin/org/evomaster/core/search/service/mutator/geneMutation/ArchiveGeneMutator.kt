package org.evomaster.core.search.service.mutator.geneMutation

import com.google.inject.Inject
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import kotlin.math.pow

/**
 * created by manzh on 2019-09-16
 *
 * this mutation is designed regarding fitness evaluation using LeftAlignmentDistance
 */
class ArchiveMutator {

    @Inject
    private lateinit var randomness: Randomness

    /**
     * enableCounter is fixed for the moment, it might be part of EMConfig or handled adaptively
     */
    private val enableCounter: Boolean = false

    private val stringCharPool: CharPool = CharPool.ALL


    fun doesSupport(gene : Gene) : Boolean{
        return gene is StringGene || gene is IntegerGene //|| gene is ObjectGene
    }

    fun mutate(gene : Gene){
        when(gene){
            is StringGene -> mutate(gene, 0.5, false)
            is IntegerGene -> mutate(gene)
            else -> TODO("NOT IMPLEMENT")
        }
    }

    private fun mutate(gene : IntegerGene){
        random(enableCounter, gene.valueMutation.counter > 3, gene.value, 0.5, (gene.valueMutation.preferMin..gene.valueMutation.preferMax).toList())
    }

    private fun mutate(gene : StringGene, probOfModifyChar : Double, priorLengthMutation : Boolean){
        if (gene.value.isEmpty() || gene.mutatedIndex == gene.value.length){
            append(gene, CharPool.WORD)
            return
        }

        if (priorLengthMutation){
            if (!gene.lengthMutation.reached){
                modifyLength(gene)
                return
            }
        }

        if (gene.charMutation.reached && !gene.lengthMutation.reached){
            when(randomness.nextBoolean(probOfModifyChar)){
                true-> modify(gene, middle = 0.5, charPool = CharPool.ALL)
                false-> modifyLength(gene)
            }
        }else{
            modify(gene, middle = 0.5, charPool = CharPool.ALL)
        }

    }

    private fun modifyLength(gene: StringGene){
        val current = gene.value.length
        val modifiedIndex = gene.mutatedIndex
        val min = if (gene.mutatedIndex > gene.lengthMutation.preferMin && modifiedIndex <= gene.lengthMutation.preferMax) gene.mutatedIndex else gene.lengthMutation.preferMin
        val validCandidates = (min..gene.lengthMutation.preferMax).toMutableList()
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
            random(enableCounter, gene.lengthMutation.counter > (if (valve < 2) 2 else valve), current, 0.5, validCandidates)

        }
        if (selected < current){
            //remove
            delete(gene, current - selected)
        }else{
            //append
            append(gene,charPool = CharPool.WORD, num = selected - current)
        }

    }

    private fun modify(gene : StringGene, charPool: CharPool, middle : Double) {
        if (gene.charMutation.reached){
            gene.charMutation.reached = false
        }else if (gene.mutatedIndex == -1){
            gene.mutatedIndex += 1
            resetChar(gene, charPool)
        }
        val current = gene.value.toCharArray()[gene.mutatedIndex].toInt()
        val validCandidates = validateCandidates(gene.charMutation.preferMin, gene.charMutation.preferMax, charPool, listOf(current))
        if (validCandidates.isEmpty())
            throw IllegalArgumentException("bug")
        if (validCandidates.size == 1){
            gene.value = modifyIndex(gene.value, gene.mutatedIndex, validCandidates.first().toChar())
        }else{

            val char = random(enableCounter, gene.charMutation.counter > 3, current, middle, validCandidates).toChar()

            gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
        }
    }

    private fun random(enableCounter : Boolean, exceed : Boolean, current: Int, probOfMiddle : Double, validCandidates: List<Int>) : Int{
        if (enableCounter){
            if (exceed)
                return findClosest(current, validCandidates)
        }

        return when(randomness.nextBoolean(probOfMiddle)){
            true -> validCandidates[validCandidates.size/2]
            false -> randomness.choose(validCandidates)
        }
    }

    private fun findClosest(current: Int, validates : List<Int>) : Int{
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

    private fun append(gene : StringGene, charPool: CharPool, num : Int = 1){
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


    private fun evaluateMutation(previous: String, current: String, mutated:StringGene, savedGene: StringGene, doesCurrentBetter: Boolean, charPool: CharPool = CharPool.ALL){
        if (savedGene != mutated){
            copyMutationInfo(savedGene, mutated)
        }
        if (previous.length != current.length){
            savedGene.lengthMutation.updateBoundary(previous.length, current.length, doesCurrentBetter)

            if (savedGene.lengthMutation.preferMin == savedGene.lengthMutation.preferMax){
                savedGene.lengthMutation.reached = true
                if (savedGene.value.isEmpty()){
                    savedGene.charMutation.reached = true
                    savedGene.mutatedIndex = 0
                }
            }
        }else{
            savedGene.charMutation.updateBoundary(previous[savedGene.mutatedIndex].toInt(), current[savedGene.mutatedIndex].toInt(), doesCurrentBetter)

            val exclude = savedGene.value[savedGene.mutatedIndex].toInt()

            if (!checkIfHasCandidates(savedGene.charMutation.preferMin, savedGene.charMutation.preferMax, charPool = charPool, exclude = listOf(exclude))){
                savedGene.charMutation.reached = true
                savedGene.mutatedIndex += 1
                savedGene.charMutation.counter = 0
                resetChar(savedGene, charPool)
            }
        }
    }

    private fun evaluateMutation(previous: Int, current: Int, mutated:IntegerGene, savedGene: IntegerGene, doesCurrentBetter: Boolean){
        if (savedGene != mutated){
            copyMutationInfo(savedGene, mutated)
        }
        savedGene.valueMutation.updateBoundary(previous, current, doesCurrentBetter)
        if (savedGene.valueMutation.preferMin == savedGene.valueMutation.preferMax){
            savedGene.valueMutation.reached = true
        }
    }

    private fun copyMutationInfo(savedGene : Gene, gene : Gene){
        when(savedGene){
            is StringGene -> copyMutationInfo(savedGene, gene as? StringGene?: throw IllegalStateException("gene should be StringGene"))
            is IntegerGene -> savedGene.valueMutation.reached = (gene as? IntegerGene?: throw IllegalStateException("gene should be IntegerGene")).valueMutation.reached
            else -> TODO("NOT IMPLEMENT")
        }
    }

    private fun copyMutationInfo(savedGene: StringGene, gene : StringGene){
        savedGene.mutatedIndex = gene.mutatedIndex
        savedGene.charMutation.reached = gene.charMutation.reached
        savedGene.lengthMutation.reached = gene.lengthMutation.reached
        /**
         * min and max might be changed based on different randomness strategies
         */
        if (savedGene.mutatedIndex == -1){
            savedGene.charMutation.preferMin = gene.charMutation.preferMin
            savedGene.charMutation.preferMax = gene.charMutation.preferMax
        }
    }

    fun resetChar(gene: StringGene, charPool: CharPool = stringCharPool){
        when(charPool){
            CharPool.WORD -> {
                gene.charMutation.preferMin = randomness.wordCharPool().first()
                gene.charMutation.preferMax = randomness.wordCharPool().last()
            }
            CharPool.ALL -> {
                gene.charMutation.preferMin = Char.MIN_VALUE.toInt()
                gene.charMutation.preferMax = Char.MAX_VALUE.toInt()
            }
            else ->  TODO("NOT IMPLEMENT")
        }
    }

    private fun validateCandidates(min : Int, max:Int, charPool: CharPool, exclude : List<Int>) : MutableList<Int>{
        val validCandidates = when(charPool){
            CharPool.WORD -> randomness.validNextWordChars(min, max).toMutableList()
            CharPool.ALL -> (min..max).toMutableList()
            else ->  TODO("NOT IMPLEMENT")
        }
        validCandidates.removeAll(exclude)
        return validCandidates
    }

    fun checkIfHasCandidates(min : Int, max:Int, charPool: CharPool = stringCharPool, exclude : List<Int>) = validateCandidates(min, max,  charPool, exclude).isNotEmpty()

}

enum class CharPool{
    ALL,
    WORD,
    LETTER
}