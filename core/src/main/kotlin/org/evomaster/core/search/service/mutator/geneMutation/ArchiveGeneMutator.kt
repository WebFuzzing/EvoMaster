package org.evomaster.core.search.service.mutator.geneMutation

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import kotlin.math.abs
import kotlin.math.max

/**
 * created by manzh on 2019-09-16
 *
 * this mutation is designed regarding fitness evaluation using LeftAlignmentDistance
 */
class ArchiveMutator {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config : EMConfig

    @Inject
    lateinit var apc : AdaptiveParameterControl

    companion object{
        private const val WITHIN_NORMAL = 0.9
    }

    fun withinNormal() : Boolean{
        return randomness.nextBoolean(WITHIN_NORMAL)
    }

    /**
     * return whether archive mutator supports this kind of gene regarding archive-based gene mutation
     */
    fun doesSupport(gene : Gene) : Boolean{
        return gene is StringGene || gene is IntegerGene || gene is ObjectGene
    }

    fun mutate(gene : Gene){
        val p = gene.copy()
        val result = when(gene){
            is StringGene -> if (!relaxIndexStringGeneMutation()) mutateFixed(gene) else mutateRelax(gene)
            is IntegerGene -> mutate(gene)
            else -> TODO("NOT IMPLEMENT")
        }
        if (p.containsSameValueAs(gene))
            throw IllegalStateException("bug $p")
    }

    /**
     * Apply archive-based mutation to select genes to mutate
     */
    fun selectGenesByArchive(genesToMutate : List<Gene>, individual: Individual, evi: EvaluatedIndividual<*>) : List<Gene>{

        val candidatesMap = genesToMutate.map { it to ImpactUtils.generateGeneId(individual, it) }.toMap()

        val genes = when(config.geneSelectionMethod){
            ImpactMutationSelection.AWAY_BAD -> selectGenesAwayBad(genesToMutate,candidatesMap,evi)
            ImpactMutationSelection.APPROACH_GOOD -> selectGenesApproachGood(genesToMutate,candidatesMap,evi)
            ImpactMutationSelection.FEED_BACK -> selectGenesFeedback(genesToMutate, candidatesMap, evi)
            ImpactMutationSelection.FOCUS_LATEST -> selectGenesFocusLatest(genesToMutate, candidatesMap, evi)
            ImpactMutationSelection.NONE -> {
                throw IllegalArgumentException("shouldn't be invoked in this fun")
            }
        }

        assert(genes.isNotEmpty())
        return genes
    }

    private fun selectGenesFocusLatest(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<*>): List<Gene>{
        if (evi.getTracking()!!.isEmpty())
            return genesToMutate

        if (evi.getTracking()!!.size > 1 && evi.mutatedGeneSpecification == null)
            throw IllegalArgumentException("mutatedGeneSpecification should be null")
        val select = if (evi.getTracking()!!.size > 1 && evi.mutatedGeneSpecification!!.mutatedGenes.isNotEmpty())
            genesToMutate.filter { evi.mutatedGeneSpecification!!.mutatedGenes.contains(it) }
        else listOf()

        if (select.isNotEmpty()) return select
        return genesToMutate
    }

    private fun selectGenesAwayBad(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<*>): List<Gene>{

        val genes =  genesToMutate.toList().map { g->
            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
        }

        return ImpactUtils.selectGenesAwayBad(genes, config.perOfCandidateGenesToMutate)
    }

    private fun selectGenesApproachGood(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<*>): List<Gene>{

        val genes =  genesToMutate.toList().map { g->
            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
        }

        return ImpactUtils.selectApproachGood(genes, config.perOfCandidateGenesToMutate)
    }


    private fun selectGenesFeedback(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<*>): List<Gene>{
        val genes =  genesToMutate.toList().map { g->
            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
        }

        return ImpactUtils.selectFeedback(genes, config.perOfCandidateGenesToMutate)
    }

    private fun decideCandidateSize(genesToMutate: List<Gene>) = (genesToMutate.size * config.perOfCandidateGenesToMutate).run {
        if(this < 1.0) 1 else this.toInt()
    }

    private fun mutate(gene : IntegerGene) : Boolean{
        val p = randomness.nextBoolean(WITHIN_NORMAL)
        val min = if (p) gene.valueMutation.preferMin else 0
        val max = if (p) gene.valueMutation.preferMax else gene.max
        //randomFromCurrentAdaptively(gene.value, min, max, hardMinValue = gene.min, hardMaxValue = gene.max, start = Math.pow(1/2, gene.max))
        return true
    }

    private fun mutateFixed(gene : StringGene, probOfModifyChar : Double = 0.8, priorLengthMutation : Boolean = false) : Boolean{
        /*
            when gene is empty or last char reaches optima, append a new char
         */
        if (gene.value.isEmpty() || gene.mutatedIndex == gene.value.length){
            append(gene, CharPool.WORD, modifyCharMutation = false)
            return false
        }

        if (priorLengthMutation){
            if (!gene.lengthMutation.reached){
                modifyLength(gene)
                return false
            }
        }

        if (gene.charsMutation.first().reached && !gene.lengthMutation.reached){

            if (gene.mutatedIndex == gene.value.length - 1){
                delete(gene, modifyCharMutation = false)
                return false
            }

            val modify = randomness.nextBoolean(probOfModifyChar)
            when(modify){
                true-> modify(gene)
                false-> modifyLength(gene)
            }
            return  modify
        }else{
            modify(gene)
            return true
        }
    }

    /**
     * mutate [gene] using archive-based method
     *
     * after sampling, it is likely that a length of the [gene] is close to optima. Thus, mutate length with a relative low probability.
     * consequently, we set a relative high value for [probOfModifyChar], i.e., default is 0.8.
     * regarding [priorLengthMutation], it might achieve a worse performance when [gene] is related to other gene,
     * e.g., fitness is about how the [gene] is close to other [Gene]. so we disable it by default.
     */
    private fun mutateRelax(gene : StringGene, probOfModifyChar : Double = 0.8, priorLengthMutation : Boolean = false) : Boolean{
        /**
         * init charsMutation
         */
        if (gene.mutatedIndex == -1 && gene.charsMutation.isEmpty()){
            gene.charsMutation.addAll((0 until gene.value.length).map { IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt()) })
        }

        /*
        if value is blank, prefer appending a new string
         */
        if (gene.value.isBlank()){
            append(gene, CharPool.WORD, modifyCharMutation = true)
            return false
        }

        if (priorLengthMutation){
            if (!gene.lengthMutation.reached){
                modifyLength(gene, strictAfter = -1, modifyCharMutation = true)
                return false
            }
        }

        val p = randomness.nextBoolean(WITHIN_NORMAL)
        val normalCharMutation = randomness.nextBoolean(probOfModifyChar)
        var doCharMutation = if (gene.charsMutation.all { it.reached }) !p else normalCharMutation
        var doLenMutation = if (gene.lengthMutation.reached) !p else !normalCharMutation

        if (doCharMutation == doLenMutation){
            if (randomness.nextBoolean())
                doCharMutation = !doCharMutation
            else
                doLenMutation = !doLenMutation
        }

        if (doCharMutation){
            val index = decideIndex(gene)
            gene.mutatedIndex = index
            modify(gene, index, gene.charsMutation[index])
        }else if(doLenMutation){
            val exclude = gene.charsMutation.mapIndexed { index, intMutationUpdate -> if (intMutationUpdate.reached && randomness.nextBoolean(WITHIN_NORMAL)) index else -1 }.filter { it > -1 }
            val last = if (exclude.isNotEmpty()) exclude.max()!! else -1
            modifyLength(gene, last, modifyCharMutation = true)
        }else
            throw IllegalStateException("should not reach")

        return doCharMutation
    }

    private fun decideIndex(gene: StringGene) : Int{
        val exclude = gene.charsMutation.mapIndexed { index, intMutationUpdate -> if (intMutationUpdate.reached) index else -1 }.filter { it > -1 }
//        val validates = (0..apc.getExploratoryValue(0, gene.value.length-1))
//        validates.filter { !exclude.contains(it) }.let {
//            if (it.isEmpty())
//                return randomness.choose(exclude)
//            else
//                return randomness.choose(it)
//        }
        var index = if (withinNormal()){
            (0 until gene.value.length).filter { !exclude.contains(it) }.min()
        }else null
        if (index != null) return index

        return randomness.nextInt(gene.value.length)
    }

    private fun modifyLength(gene: StringGene){
        modifyLength(gene, gene.mutatedIndex, false)
    }

    private fun modifyLength(gene: StringGene, strictAfter : Int, modifyCharMutation : Boolean){
        val current = gene.value.length
        val min = if (strictAfter > gene.lengthMutation.preferMin && strictAfter <= gene.lengthMutation.preferMax) strictAfter else gene.lengthMutation.preferMin
        val validCandidates = validateCandidates(min, gene.lengthMutation.preferMax, listOf(current))

        val p = randomness.nextBoolean()
        if (validCandidates == 0){
            when{
                gene.value.length == gene.maxLength || !p -> {
                    delete(gene, modifyCharMutation = modifyCharMutation)
                }
                gene.value.isBlank() || p -> {
                    append(gene, CharPool.WORD, modifyCharMutation = modifyCharMutation)
                }
            }
        }else{
            val normal = withinNormal()
            when{
                current == gene.maxLength || (current == gene.lengthMutation.preferMax && normal)|| !p -> {
                    delete(gene, modifyCharMutation = modifyCharMutation)
                }else ->{
                    val start = (if (!normal || current > gene.lengthMutation.preferMax) gene.maxLength - current else gene.lengthMutation.preferMax - current)
                    val delta = apc.getExploratoryValue( start=start,end = 1)
                    append(gene, CharPool.WORD, num = delta, modifyCharMutation = modifyCharMutation)
                }
            }
        }

        if (current == gene.value.length || gene.value.length !in (gene.minLength..gene.maxLength))
            throw IllegalArgumentException("mutate a length of the gene improperly")
    }

    /*
    regarding char mutation, we prefer to mutate relative closer one instead of making a jump in middle.
     */
    private fun modify(gene : StringGene) {
        if (gene.charsMutation.first().reached){
            gene.charsMutation.first().reached = false
        }else if (gene.mutatedIndex == -1){
            gene.mutatedIndex += 1
            resetCharMutationUpdate(gene.charsMutation.first())
        }
        modify(gene, gene.mutatedIndex, gene.charsMutation.first())
    }

    private fun modify(gene : StringGene, index:Int, charMutation : IntMutationUpdate) {
        val current = gene.value.toCharArray()[index].toInt()

        when (validateCandidates(charMutation.preferMin, charMutation.preferMax, listOf(current))) {
            0 -> {
                //FIXME
                if (!charMutation.reached)
                    throw IllegalArgumentException("validCandidates can only be empty when selected is optimal")
                val char = randomFromCurrentAdaptively(current, Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt(), Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt(), start = 6, end = 3).toChar()
                gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
            }
            1 -> gene.value = modifyIndex(gene.value, gene.mutatedIndex, (charMutation.preferMin..charMutation.preferMax).first{it != current}.toChar())
            else -> {
                val char = //(random(false, false, current, 0.5, (charMutation.preferMin..charMutation.preferMax).filter { it != current })).toChar()
                randomFromCurrentAdaptively(current, charMutation.preferMin, charMutation.preferMax, Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt(), start = 6, end = 3).toChar()
                gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
            }
        }
        if (current == gene.value.toCharArray()[index].toInt())
            throw IllegalArgumentException("mutate a char of the gene improperly")
    }

    private fun random(enableCounter : Boolean, exceed : Boolean, current: Int, probOfMiddle : Double, validCandidates: List<Int>) : Int{
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

    /**
     * during later phase of search, modify char/int with relative close genes
     */
    private fun randomFromCurrentAdaptively(current: Int, minValue: Int, maxValue: Int, hardMinValue : Int, hardMaxValue : Int, start : Int, end : Int) : Int{
        //val middle = (maxValue - minValue)/2
        val normal = withinNormal()
        val range = if (normal) max(abs(maxValue - current), abs(minValue - current)).toLong() else (hardMaxValue - hardMinValue).toLong()
        val delta = GeneUtils.getDelta(randomness, apc, range = range, start =start, end = end)
        val value = if (normal && current + delta > maxValue)
            current - delta
        else if (normal && current - delta < minValue)
             current + delta
        else current + delta * randomness.choose(listOf(-1, 1))

        return if (value < hardMinValue) hardMinValue.run { if (this == current) this + 1 else this } else if (value > hardMaxValue) hardMaxValue.run { if (this == current) this - 1 else this } else value
    }

    private fun delete(gene : StringGene, num: Int = 1, modifyCharMutation : Boolean){
        if (modifyCharMutation){
            assert(gene.value.length == gene.charsMutation.size)
            (0 until num).forEach {
                gene.charsMutation.removeAt(gene.charsMutation.size - 1)
            }
        }
        gene.value = gene.value.dropLast(num)

        if (modifyCharMutation && gene.charsMutation.size != gene.value.length){
            throw IllegalArgumentException("bug")
        }
    }

    private fun append(gene : StringGene, charPool: CharPool, num : Int = 1, modifyCharMutation : Boolean){
        if (modifyCharMutation){
            assert(gene.value.length == gene.charsMutation.size)
        }
        (0 until num).forEach { _ ->
            gene.value += when(charPool){
                CharPool.WORD -> randomness.nextWordChar()
                CharPool.ALL -> randomness.nextChar(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())
            }
            if (modifyCharMutation)
                gene.charsMutation.add(IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt()))
        }

        if (modifyCharMutation && gene.charsMutation.size != gene.value.length){
            throw IllegalArgumentException("bug")
        }
    }

    private fun modifyIndex(value : String, index : Int, char: Char) : String{
        if (index >= value.length) throw IllegalArgumentException("$index exceeds the length of $value")
        return value.toCharArray().also {it[index] = char }.joinToString("")
    }


    fun resetCharMutationUpdate(charMutation: IntMutationUpdate){
        charMutation.preferMin = Char.MIN_VALUE.toInt()
        charMutation.preferMax = Char.MAX_VALUE.toInt()
    }

    /**
     * [min] and [max] are inclusive
     */
    private fun validateCandidates(min : Int, max:Int, exclude : List<Int>) : Int {
        return (min..max).filter { !exclude.contains(it) }.size
    }

    fun checkIfHasCandidates(min : Int, max:Int, exclude : List<Int>) = validateCandidates(min, max, exclude) > 0

    fun relaxIndexStringGeneMutation() : Boolean = true

    fun createCharMutationUpdate() = IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())

    fun createLengthMutationUpdate(gene : StringGene) = IntMutationUpdate(gene.minLength, gene.maxLength)
}

enum class CharPool{
    ALL,
    WORD
}