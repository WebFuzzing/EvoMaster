package org.evomaster.core.search.service.mutator.geneMutation

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        const val WITHIN_NORMAL = 0.9
        private val log: Logger = LoggerFactory.getLogger(ArchiveMutator::class.java)
    }

    fun withinNormal(prob : Double = WITHIN_NORMAL) : Boolean{
        return randomness.nextBoolean(prob)
    }

    /**
     * return whether archive mutator supports this kind of gene regarding archive-based gene mutation
     */
    fun doesSupport(gene : Gene) : Boolean{
        return gene is StringGene  || gene is ObjectGene || ParamUtil.getValueGene(gene) is StringGene || ParamUtil.getValueGene(gene) is ObjectGene //|| gene is IntegerGene
    }

    fun mutate(gene : Gene){
        val p = gene.copy()
        when(gene){
            is StringGene -> mutate(gene)
//            is IntegerGene -> mutate(gene)
//            is EnumGene<*> -> mutate(gene)
            else -> {
                if (ParamUtil.getValueGene(gene) is StringGene){
                    mutate(ParamUtil.getValueGene(gene))
                }
                else{
                    log.warn("not implemented error")
                }
            }
        }
        if (p.containsSameValueAs(gene))
            log.warn("value of gene shouldn't be same with previous")
    }

    /**
     * Apply archive-based mutation to select genes to mutate
     */
    fun selectGenesByArchive(genesToMutate : List<Gene>, individual: Individual, evi: EvaluatedIndividual<*>) : List<Gene>{

        val candidatesMap = genesToMutate.map { it to ImpactUtils.generateGeneId(individual, it) }.toMap()

        val collected =  genesToMutate.toList().map { g->
            val id = candidatesMap[g]?:throw IllegalArgumentException("mismatched")
            Pair(g, evi.getImpactOfGenes(id)!!)
        }

        val genes = if (enableArchiveSelection()){
            selectGenesByArchive(collected, config.perOfCandidateGenesToMutate)
        } else
            genesToMutate

        if (genes.isEmpty()){
            log.warn("Archive-based mutation should not produce empty genes to mutate")
            return genesToMutate
        }
        return genes
    }

    fun <T> selectGenesByArchive(genes: List<Pair<T, Impact>>, percentage : Double) : List<T>{
        val method = decideArchiveGeneSelectionMethod(genes.map { it.second })
        val selects = when(method){
            ImpactMutationSelection.AWAY_NOIMPACT -> ImpactUtils.selectGenesAwayBad(genes, percentage = percentage, prioritizeNoVisit = true)
            ImpactMutationSelection.APPROACH_IMPACT_N -> ImpactUtils.selectApproachGood(genes, percentage = percentage, prioritizeNoVisit = true)
            ImpactMutationSelection.APPROACH_IMPACT_I -> ImpactUtils.selectApproachGood2(genes, percentage = percentage, prioritizeNoVisit = true)
            else -> {
                genes.map { it.first }
            }
        }
        if (selects.isEmpty()){
            log.warn("Archive-based mutation should not produce empty genes to mutate")
            return genes.map { it.first }
        }
        return selects
    }

    fun decideArchiveGeneSelectionMethod(genes : List<Impact>) : ImpactMutationSelection {
        return when (config.adaptiveGeneSelection) {
            EMConfig.AdaptiveSelection.FIXED_SELECTION -> config.geneSelectionMethod
            EMConfig.AdaptiveSelection.RANDOM -> randomGeneSelectionMethod()
            //EMConfig.AdaptiveSelection.GUIDED -> methodGuidedByImpact(genes)
        }
    }

    private fun randomGeneSelectionMethod() : ImpactMutationSelection
            = randomness.choose(listOf(ImpactMutationSelection.APPROACH_IMPACT_N, ImpactMutationSelection.APPROACH_IMPACT_I, ImpactMutationSelection.AWAY_NOIMPACT))

//    private fun methodGuidedByImpact(genes : List<Pair<Gene, GeneImpact>>) : ImpactMutationSelection{
//
//        val sortedWithDegree = genes.map { it.second.timesOfImpact/it.second.timesToManipulate.toDouble() }.sorted()
//        val distance = abs(sortedWithDegree.first() - sortedWithDegree.last())
//        if (distance > 0.5){
//            val good = sortedWithDegree.count { it > 0.5 }
//            if (good < sortedWithDegree.size * 0.3)
//                return ImpactMutationSelection.APPROACH_IMPACT
//            else
//                return ImpactMutationSelection.AWAY_NOIMPACT
//        }
//        return ImpactMutationSelection.FEEDBACK_DIRECT
//    }

    private fun selectGenesFocusLatest(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<*>): List<Gene>{
        if (evi.getTracking()!!.isEmpty())
            return genesToMutate

        if (evi.getTracking()!!.size > 1 && evi.mutatedGeneSpecification == null)
            log.warn("mutatedGeneSpecification should be null")
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

//    private fun selectGenesFeedback(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<*>): List<Gene>{
//        val genes =  genesToMutate.toList().map { g->
//            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
//        }
//
//        return ImpactUtils.selectFeedback(genes, config.perOfCandidateGenesToMutate)
//    }

    private fun mutate(gene : IntegerGene){
//        val value = mutate(
//                gene,
//                gene.value,
//                gene.valueMutation,
//                gene.min,
//                gene.max,
//                15,
//                5
//        )
//        gene.value = value
    }

//    private fun mutate(gene : EnumGene<*>) {
//        val index = mutate(gene, gene.index, gene.optionMutationUpdate, 0, gene.values.size, 2, 1)
//        gene.index = index
//    }

    private fun mutate(independence : GeneIndependenceInfo, current: Int, update: IntMutationUpdate, hardMinValue: Int, hardMaxValue: Int, slightStart : Int, slightEnd : Int) : Int{
        val preferSlight = approachSlightMutation(independence)
        return if (preferSlight){
            randomFromCurrentAdaptively(current= current, minValue = update.preferMin, maxValue = update.preferMax, hardMinValue = hardMinValue, hardMaxValue = hardMaxValue,
                    start = slightStart, end = slightEnd)
        }else{
            preferMiddle(update.preferMin, update.preferMax, current)
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
    private fun mutate(gene : StringGene, probOfModifyChar : Double = 0.8, priorLengthMutation : Boolean = false){
        /**
         * init charsMutation
         */
        if (gene.mutatedIndex == -2){
            if (gene.charsMutation.isNotEmpty()){
                log.warn("duplicated Initialized")
                if (gene.charsMutation.size != gene.value.length){
                    gene.charsMutation.clear()
                    gene.charsMutation.addAll((0 until gene.value.length).map { createCharMutationUpdate() })
                }
            }else
                gene.charsMutation.addAll((0 until gene.value.length).map { createCharMutationUpdate() })

            gene.charMutationInitialized()
        }

        /*
        if value is blank, prefer appending a new string
         */
        if (gene.value.isBlank()){
            append(gene, CharPool.WORD, modifyCharMutation = true)
            return
        }

        if (priorLengthMutation){
            if (!gene.lengthMutation.reached){
                modifyLength(gene, strictAfter = -1, modifyCharMutation = true)
                return
            }
        }

        val p = withinNormal()

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
            log.warn("at least one of doCharMutation {} and doLenMutation {} should be enabled", doCharMutation, doLenMutation)

    }

    private fun approachPrefer(gene: StringGene) : Boolean{
        return when(config.archiveGeneMutation){
            EMConfig.ArchiveGeneMutation.SPECIFIED -> withinNormal()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> withinNormal(gene.dependencyInfo.degreeOfIndependence)
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    /*
        if Independence is higher, far away from approachSlightMutation
        if Independence is lower, close to approachSlightMutation
     */
    private fun approachSlightMutation(gene: StringGene) : Boolean{
        return when(config.archiveGeneMutation){
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> gene.dependencyInfo.resetTimes > 0 && withinNormal()
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    private fun approachSlightMutation(info: GeneIndependenceInfo) : Boolean{
        return when(config.archiveGeneMutation){
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> info.resetTimes > 0 && withinNormal()
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    private fun decideIndex(gene: StringGene) : Int{
        val exclude = gene.charsMutation.mapIndexed { index, intMutationUpdate -> if (intMutationUpdate.reached) index else -1 }.filter { it > -1 }

        var index = if (approachPrefer(gene)){
            (0 until gene.value.length).filter { !exclude.contains(it) }.min()
        }else null
        if (index != null) return index

        return randomness.nextInt(gene.value.length)
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
            val normal = approachPrefer(gene)//withinNormal(gene.degreeOfIndependency)
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
            log.warn("length of value of string gene should be changed after length mutation: previous {} vs. current {}", current, gene.value.length)
    }

    private fun modify(gene : StringGene, index:Int, charMutation : IntMutationUpdate) {
        val current = gene.value.toCharArray()[index].toInt()

        when (validateCandidates(charMutation.preferMin, charMutation.preferMax, listOf(current))) {
            0 -> {
                if (!charMutation.reached)
                    log.warn("validCandidates can only be empty when selected is optimal")
                val char = randomFromCurrentAdaptively(
                        current,
                        Char.MIN_VALUE.toInt(),
                        Char.MAX_VALUE.toInt(),
                        Char.MIN_VALUE.toInt(),
                        Char.MAX_VALUE.toInt(),
                        start = 6,
                        end = 3).toChar()
                gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
            }
            1 -> gene.value = modifyIndex(gene.value, gene.mutatedIndex, (charMutation.preferMin..charMutation.preferMax).first{it != current}.toChar())
            else -> {
                val char =
                        if(approachSlightMutation(gene)) //prefer middle if the degree of independent is quite high
                            randomFromCurrentAdaptively(
                                    current,
                                    charMutation.preferMin,
                                    charMutation.preferMax,
                                    Char.MIN_VALUE.toInt(),
                                    Char.MAX_VALUE.toInt(),
                                    start = 6,
                                    end = 3).toChar()
                        else
                            preferMiddle(charMutation.preferMin,charMutation.preferMax,current).toChar()

                gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
            }
        }
        if (current == gene.value.toCharArray()[index].toInt())
            log.warn("char should be modified after char mutation: previous {} vs. current {}", current, gene.value.toCharArray()[index].toInt())
    }

    private fun preferMiddle(min: Int, max: Int, current: Int) : Int{
        if (min > max)
            log.warn("min {} should not be more than max {}", min, max)
        val cand =  (min..max).filter { it != current }.toList()
        return if (withinNormal()) cand[cand.size/2] else randomness.choose(cand)
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
        val prefer = withinNormal()
        val range = max(abs((if (prefer) maxValue else hardMaxValue) - current), abs((if (prefer)minValue else hardMinValue) - current)).toLong()
        val delta = GeneUtils.getDelta(randomness, apc, range = range, start =start, end = end)
        val value = if (prefer && current + delta > maxValue)
            current - delta
        else if (prefer && current - delta < minValue)
             current + delta
        else
            current + delta * randomness.choose(listOf(-1, 1))

        return when{
            value < hardMinValue -> if (current == hardMinValue) hardMinValue + 1 else hardMinValue
            value > hardMaxValue -> if (current == hardMaxValue) hardMaxValue - 1 else hardMaxValue
            else -> value
        }
    }

    private fun delete(gene : StringGene, num: Int = 1, modifyCharMutation : Boolean){
        if (modifyCharMutation && gene.value.length != gene.charsMutation.size){
            log.warn("regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", gene.value.length, gene.value, gene.charsMutation.size)
        }
        val value = gene.value
        val expected = value.length - num
        gene.value = value.dropLast(num)

        if (modifyCharMutation){
            if (num == 0)
                log.warn("mutated length of the gene should be more than 0")
            (0 until num).forEach { _ ->
                gene.charsMutation.removeAt(expected)
            }
        }
        if (modifyCharMutation && gene.value.length != gene.charsMutation.size){
            log.warn("{} are deleted:regarding string gene, a length {} of a value {} of the gene should be always same with a size {} of its charMutation", num, gene.value.length, gene.value, gene.charsMutation.size)
        }
    }

    private fun append(gene : StringGene, charPool: CharPool, num : Int = 1, modifyCharMutation : Boolean){
        if (modifyCharMutation && gene.value.length != gene.charsMutation.size){
            log.warn("regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", gene.value.length, gene.value, gene.charsMutation.size)
        }

        if (num == 0)
            log.warn("mutated length of the gene should be more than 0")

        (0 until num).forEach { _ ->
            gene.value += when(charPool){
                CharPool.WORD -> randomness.nextWordChar()
                CharPool.ALL -> randomness.nextChar(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())
            }
        }
        if (modifyCharMutation)
            gene.charsMutation.addAll((0 until num).map { createCharMutationUpdate() })

        if (modifyCharMutation && gene.value.length != gene.charsMutation.size){
            log.warn("{} are appended:regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", num, gene.value.length, gene.value, gene.charsMutation.size)
        }
    }

    private fun modifyIndex(value : String, index : Int, char: Char) : String{
        if (index >= value.length) throw IllegalArgumentException("$index exceeds the length of $value")
        return value.toCharArray().also {it[index] = char }.joinToString("")
    }

    /**
     * [min] and [max] are inclusive
     */
    fun validateCandidates(min : Int, max:Int, exclude : List<Int>) : Int {
        if (max < min)
            return 0
        return (min..max).filter { !exclude.contains(it) }.size
    }

    fun enableArchiveSelection() = (config.geneSelectionMethod != ImpactMutationSelection.NONE || config.adaptiveGeneSelection != EMConfig.AdaptiveSelection.FIXED_SELECTION)
            && randomness.nextBoolean(config.probOfArchiveMutation)

    fun enableArchiveGeneMutation() = config.probOfArchiveMutation > 0 && config.archiveGeneMutation != EMConfig.ArchiveGeneMutation.NONE

    fun createCharMutationUpdate() = IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())
}

enum class CharPool{
    ALL,
    WORD
}