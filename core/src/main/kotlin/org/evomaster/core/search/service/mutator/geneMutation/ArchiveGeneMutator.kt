package org.evomaster.core.search.service.mutator.geneMutation

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.math.max

/**
 *
 * Archive-based mutator which handle
 * - archive-based gene selection to mutate
 * - mutate the selected genes based on their performance (i.e., results of fitness evaluation)
 * - besides, string mutation is designed regarding fitness evaluation using LeftAlignmentDistance
 */
class ArchiveMutator {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config : EMConfig

    @Inject
    lateinit var apc : AdaptiveParameterControl

    companion object{

        private val log: Logger = LoggerFactory.getLogger(ArchiveMutator::class.java)

        const val WITHIN_NORMAL = 0.9

        const val DEP_THRESHOLD = 0

        /**
         * control maximum length mutation for a string, otherwise, it is quite expensive.
         */
        const val MAX_STRING_LEN_MUTATION = 64
    }

    /**************************** gene selection ********************************************/

    /**
     * Apply archive-based mutation to select [genes] from [individual] to mutate regarding their impacts saved in [evi],
     * the applied selection method can be recorded in [mutatedGenes] if it is not null
     */
    fun selectGenesByArchive(genesToMutate : List<Gene>, individual: Individual, evi: EvaluatedIndividual<*>, targets : Set<Int>, mutatedGenes: MutatedGeneSpecification?) : List<Gene>{

        val candidatesMap = genesToMutate.map { it to ImpactUtils.generateGeneId(individual, it) }.toMap()

        val collected =  genesToMutate.toList().map { g->
            val id = candidatesMap[g]?:throw IllegalArgumentException("mismatched")
            if(evi.getImpactOfGenes(id) == null)
                throw IllegalArgumentException("cannot find geneImpact info with id $id")
            Pair(g, evi.getImpactOfGenes(id)!!)
        }

        val genes = if (applyArchiveSelection()){
            selectGenesByArchive(collected, config.perOfCandidateGenesToMutate, targets, mutatedGenes)
        } else {
            return genesToMutate
        }

        if (genes.isEmpty()){
            log.warn("Archive-based mutation should not produce empty genes to mutate")
            return genesToMutate
        }

        return genes
    }

    private fun selectSortedGenes(genesToMutate: List<Gene>, n : Int) : List<Gene>{
        if (genesToMutate.size == n) return genesToMutate
        if (genesToMutate.size < n) throw IllegalStateException("required number ($n) of selected genes is more than available genes (${genesToMutate.size}).")
        //probability is [0.1, 0.9]
        val selected = mutableListOf<Gene>()
        var counter = 0
        while (selected.size < n && counter / genesToMutate.size < 3){
            val rank = counter%genesToMutate.size
            val p = 0.1 + (0.9 - 0.1) * (genesToMutate.size - rank)/genesToMutate.size
            if (randomness.nextDouble() < p)
                selected.add(genesToMutate[rank])
            counter++
        }
        if (selected.size == n) return selected

        return genesToMutate.subList(0, n)
    }

    /**
     * Apply archive-based mutation to select [genes] which contain their impact info.
     * This fun can be used for selecting a subset of genes for individual or composite gene
     * (e.g., select a field to mutate for ObjectGene).
     * [percentage] controls a selected percentage from [genes], but it varies with regards to different [GeneMutationSelectionMethod]
     * the applied selection method can be recorded in [mutatedGenes] if it is not null
     */
    fun <T> selectGenesByArchive(genes: List<Pair<T, Impact>>, percentage : Double, targets : Set<Int>,mutatedGenes: MutatedGeneSpecification? = null) : List<T>{
        val method = decideArchiveGeneSelectionMethod(genes.map { it.second })
        if (method.adaptive)
            throw IllegalArgumentException("the decided method should be a fixed method")
        mutatedGenes?.geneSelectionStrategy = method
        val selects = when(method){
            GeneMutationSelectionMethod.AWAY_NOIMPACT -> selectGenesAwayNoimpact(genes, percentage = percentage, prioritizeNoVisit = true)
            //GeneMutationSelectionMethod.APPROACH_IMPACT_N -> selectApproachImpactWithN(genes, percentage = percentage, prioritizeNoVisit = true)
            GeneMutationSelectionMethod.APPROACH_IMPACT_I -> selectApproachImpact(genes, percentage = percentage, targets = targets, prioritizeNoVisit = true)
            //GeneMutationSelectionMethod.FEED_DIRECT_IMPACT -> ImpactUtils.selectFeedDirect(genes, percentage = percentage, prioritizeNoVisit = true)
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

    /**
     * decide an archive-based gene selection method when the selection is adaptive (i.e., [GeneMutationSelectionMethod.adaptive])
     */
    private fun decideArchiveGeneSelectionMethod(genes : List<Impact>) : GeneMutationSelectionMethod {
        return when (config.geneSelectionMethod) {
            GeneMutationSelectionMethod.ALL_FIXED_RAND -> randomGeneSelectionMethod()
            else -> config.geneSelectionMethod
        }
    }

    private fun randomGeneSelectionMethod() : GeneMutationSelectionMethod
            = randomness.choose(GeneMutationSelectionMethod.values().filter { !it.adaptive && it.archive })

    /**
     * this fun is used by [Archive] to sample an individual from population (i.e., [individuals])
     * if [ArchiveMutator.enableArchiveSelection] is true.
     * In order to identify impacts of genes, we prefer to select an individual which has some impact info.
     */
    fun <T : Individual> selectIndividual(individuals : List<EvaluatedIndividual<T>>) : EvaluatedIndividual<T>{
        if (randomness.nextBoolean(0.1)) return randomness.choose(individuals)
        val impacts = individuals.filter { it.getImpactOfGenes().any { i->i.value.timesToManipulate > 0 } }
        if (impacts.isNotEmpty()) return randomness.choose(impacts)
        return randomness.choose(individuals)
    }

    private fun <T> prioritizeNoVisit(genes : List<Pair<T, Impact>>): List<T>{
        return genes.filter { it.second.timesToManipulate == 0 }.map { it.first }
    }

    private fun <T> selectGenesAwayNoimpact(genes : List<Pair<T, Impact>>, percentage : Double, prioritizeNoVisit : Boolean = true) : List<T>{
        if (genes.size == 1) return listOf(genes.first().first)
        if (prioritizeNoVisit) prioritizeNoVisit(genes).let { if (it.isNotEmpty()) return listOf(it.first()) }
        val size = decideSize(genes.size, percentage)
        return genes.sortedBy { it.second.timesOfNoImpacts }.subList(0, genes.size - size).map { it.first }
    }

    private fun <T>selectApproachImpact(genes : List<Pair<T, Impact>>, percentage : Double, targets: Set<Int>, prioritizeNoVisit : Boolean = true) : List<T>{
        if (prioritizeNoVisit) prioritizeNoVisit(genes).let { if (it.isNotEmpty()) return listOf(it.first()) }
        val size = decideSize(genes.size, percentage)
        return sortGenes(genes, targets).subList(0, size)
    }


    private fun <T> sortGenes(genes : List<Pair<T, Impact>>, targets : Set<Int>) : List<T>{

        //higher number is prior
        val impacts = genes.sortedByDescending { it.second.timesOfImpact.filter { e-> targets.contains(e.key) }.map { e-> e.value }.max()?:0 }

        //lower number is prior, i.e., prioritize to mutate genes which have latest impacts
        val noImpactsFromImpact = genes.sortedBy { it.second.noImpactFromImpact.filter { e-> targets.contains(e.key) }.map { e-> e.value }.min()?: Int.MAX_VALUE}

        //lower number is prior, i.e., prioritize to mutate genes which achieved latest improvement
        val noImprovement = genes.sortedBy { it.second.noImprovement.filter { e-> targets.contains(e.key) }.map { e-> e.value }.min()?: Int.MAX_VALUE}

        return genes.sortedBy { g-> impacts.indexOf(g) + noImpactsFromImpact.indexOf(g) + noImprovement.indexOf(g) }.map{ it.first }
    }

    private fun decideSize(list : Int, percentage : Double) = (list * percentage).run {
        if(this < 1.0) 1 else this.toInt()
    }
    /**************************** gene mutation ********************************************/

    fun withinNormal(prob : Double = WITHIN_NORMAL) : Boolean{
        return randomness.nextBoolean(prob)
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
        /**
         * with 10% probability, mutate char even all genes reach optima
         */
        var doCharMutation = if (gene.charsMutation.all { it.reached }) !p else normalCharMutation
        /**
         * with 10% probability, mutate length even current is optimal
         */
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

    private fun approachSlightMutation(gene: StringGene) : Boolean{
        return when(config.archiveGeneMutation){
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            /*
            if Independence is higher, far away from approachSlightMutation
            if Independence is lower, close to approachSlightMutation
            */
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> gene.dependencyInfo.resetTimes > DEP_THRESHOLD && withinNormal()
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalStateException("bug!")
        }
    }

    private fun approachSlightMutation(info: GeneIndependenceInfo) : Boolean{
        return when(config.archiveGeneMutation){
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> info.resetTimes > 0 && withinNormal()
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    /**
     * one adaptive point
     */
    private fun decideIndex(gene: StringGene) : Int{
        if (approachPrefer(gene)){
            /**
             * first index of char that has not reached optima yet
             */
            gene.charsMutation.indexOfFirst {
                !it.reached
            }.let {
                if (it != -1) return it
            }
        }
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
                    val delta = apc.getExploratoryValue( start= if(start > MAX_STRING_LEN_MUTATION) MAX_STRING_LEN_MUTATION else start,end = 1)
                    append(gene, CharPool.WORD, num = delta, modifyCharMutation = modifyCharMutation)
                }
            }
        }

        if (current == gene.value.length || (gene.value.length < gene.minLength && gene.value.length > gene.maxLength))
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
            1 -> gene.value = modifyIndex(gene.value, gene.mutatedIndex, (charMutation.preferMin..charMutation.preferMax).toMutableList().first{it != current}.toChar())
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
        val cand = (if (current == min) ((min+1)..max)
        else if (current == max) min until max
        else if (current in min..max) (min until current).plus((current+1)..max)
        else min..max).toList()

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

        gene.value += String((0 until num).map { randomness.nextWordChar() }.toCharArray())
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
        if (max == min && exclude.contains(min)) return 0
        if (max == min)
            return 1
        return  max - min + 1 - exclude.filter { it>= min || it <=max }.size
    }

    /**************************** utilities ********************************************/

    fun applyArchiveSelection() = enableArchiveSelection()
            && randomness.nextBoolean(config.probOfArchiveMutation)

    fun enableArchiveSelection() = config.geneSelectionMethod != GeneMutationSelectionMethod.NONE && config.probOfArchiveMutation > 0.0

    fun enableArchiveGeneMutation() = config.probOfArchiveMutation > 0 && config.archiveGeneMutation != EMConfig.ArchiveGeneMutation.NONE

    fun enableArchiveMutation() = enableArchiveGeneMutation() || enableArchiveSelection()

    fun createCharMutationUpdate() = IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())

    /**
     * export impact info collected during search that is normally used for experiment
     */
    fun exportImpacts(solution: Solution<*>){
        val path = Paths.get(config.impactFile)
        Files.createDirectories(path.parent)

        val content = mutableListOf<String>()
        content.add(mutableListOf("test","rootGene").plus(Impact.toCSVHeader()).joinToString(","))
        solution.individuals.forEachIndexed { index, e->
            e.getImpactOfGenes().forEach { (t, geneImpact) ->
                content.add(mutableListOf(index.toString(), t).plus(geneImpact.toCSVCell()).joinToString(","))
                geneImpact.flatViewInnerImpact().forEach{ (name, impact) ->
                    content.add(mutableListOf(index.toString(), "$t-$name").plus(impact.toCSVCell()).joinToString(","))
                }
            }
        }
        if (content.size > 1){
            Files.write(path, content)
        }
    }
}

enum class CharPool{
    ALL,
    WORD
}