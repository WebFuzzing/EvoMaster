package org.evomaster.core.search.service.mutator.geneMutation

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.math.abs
import kotlin.math.max

/**
 * Archive-based mutator which handle
 * - archive-based gene selection to mutate
 * - mutate the selected genes based on their performance (i.e., results of fitness evaluation)
 * - besides, string mutation is designed regarding fitness evaluation using LeftAlignmentDistance
 */
class ArchiveMutator {

    @Inject
    private lateinit var time : SearchTimeController

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    @Inject
    lateinit var apc: AdaptiveParameterControl

    companion object {

        private val log: Logger = LoggerFactory.getLogger(ArchiveMutator::class.java)

        const val WITHIN_NORMAL = 0.9

        const val DEP_THRESHOLD = 0

        /**
         * control maximum length mutation for a string, otherwise, it is quite expensive.
         */
        const val MAX_STRING_LEN_MUTATION = 64
    }

    /**************************** gene selection ********************************************/

    fun calculateWeightByArchive(genesToMutate : List<Gene>, map: MutableMap<Gene, Double> ,individual: Individual?, impacts: List<Impact>?, evi: EvaluatedIndividual<*>, targets : Set<Int>){
        TODO()// when merge with archive-based branch
    }


    /**
     * Apply archive-based mutation to select [genes] from [individual] to mutate regarding their impacts saved in [evi],
     * the applied selection method can be recorded in [mutatedGenes] if it is not null
     */
    fun selectGenesByArchive(
            genesToMutate: List<Gene>,
            individual: Individual,
            evi: EvaluatedIndividual<*>,
            targets: Set<Int>,
            mutatedGenes: MutatedGeneSpecification?
    ): List<Gene> {

        val collected = genesToMutate.map { g ->
            val impact = evi.getImpact(individual, g)
                    ?: throw IllegalArgumentException("mismatched gene and impact info during mutation")
            Pair(g, impact)
        }
        val n = mutationSpace(individual, evi)
        return selectGenesByArchive(collected, n, targets, mutatedGenes)
    }

    private fun mutationSpace(individual: Individual, evi: EvaluatedIndividual<*>): Int {
        val spaceSize = when (config.geneMutationStrategy) {
            EMConfig.GeneMutationStrategy.ONE_OVER_N -> individual.seeGenes(Individual.GeneFilter.ALL).filter { it.isMutable() }.count()
            EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL -> individual.seeGenes(Individual.GeneFilter.NO_SQL).filter { it.isMutable() }.count()
        }
        return max(2, spaceSize)
    }

    private fun <T> selectWithSorted(candidates: List<T>, mutationSpace: Int): List<T> {
        if (candidates.size == 1) return candidates
        val total = candidates.size
        val times = max(1.0, total / mutationSpace.toDouble())
        val totalWeight = 1.0 / (total * (total + 1) / 2.0)

        val selected = mutableListOf<T>()
        var counter = 0
        while (selected.isEmpty() && counter < 5) {
            (0 until total).forEach { index ->
                if (randomness.nextBoolean(totalWeight * (total - index) * times)) {
                    selected.add(candidates[index])
                }
            }
            counter++
        }
        if (selected.isEmpty()) {
            return selectNSorted(candidates, 1) //selected.add(candidates.first())
        }

        return selected
    }


    private fun <T> selectNSorted(candidates: List<T>, num: Int, isMax: Boolean = true): List<T> {
        val n = if (isMax) randomness.nextInt(1, num) else num
        if (candidates.size == n) return candidates
        if (candidates.size < n) throw IllegalStateException("required number ($n) of selected genes is more than available candidates (${candidates.size}).")
        //probability is [0.1, 0.9]
        val selected = mutableListOf<T>()
        var counter = 0
        while (selected.size < n && counter / candidates.size < 3) {
            val rank = counter % candidates.size
            val p = 0.1 + (0.9 - 0.1) * (candidates.size - rank) / candidates.size
            if (randomness.nextDouble() < p)
                selected.add(candidates[rank])
            counter++
        }
        if (selected.size == n) return selected

        return candidates.subList(0, n)
    }

    /**
     * Apply archive-based mutation to select [genes] which contain their impact info.
     * This fun can be used for selecting a subset of genes for individual or composite gene
     * (e.g., select a field to mutate for ObjectGene).
     * [percentage] controls a selected percentage from [genes], but it varies with regards to different [GeneMutationSelectionMethod]
     * the applied selection method can be recorded in [mutatedGenes] if it is not null
     */
    fun <T> selectGenesByArchive(genes: List<Pair<T, Impact>>, mutationSizeN: Int = genes.size, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification? = null): List<T> {
        val noVisit = prioritizeNoVisit(genes)

        val method = decideArchiveGeneSelectionMethod(genes.map { it.second })
        if (method.adaptive)
            throw IllegalArgumentException("the decided method should be a fixed method")
        mutatedGenes?.geneSelectionStrategy = method

        val selects = sortGenesBasedOnImpacts(genes.filterNot { noVisit.contains(it) }, method, targets).toMutableList()

        val sorted = if (config.prioritizeNotVisit)
            noVisit.plus(selects)
        else {
            noVisit.forEach { p ->
                val index = randomness.nextInt(0, max(0, selects.size - 1))
                selects.add(index, p)
            }
            selects
        }

        //FIXME how to select a gene from genes with same impact info
        val group = sorted.groupBy { it.second }.map { l-> randomness.choose(l.value).first }

        val percentage = when (config.impactGeneSelection) {
            EMConfig.ImpactGeneSelection.PROBABILITY -> 1.0
            EMConfig.ImpactGeneSelection.SUBSET_PROBABILITY -> apc.getExploratoryValue(start = config.startPerOfCandidateGenesToMutate, end = config.endPerOfCandidateGenesToMutate)
            EMConfig.ImpactGeneSelection.ADAPTIVE_SUBSET_PROBABILITY -> {
                val prob = apc.getExploratoryValue(start = config.startPerOfCandidateGenesToMutate, end = config.endPerOfCandidateGenesToMutate)
                //TODO modify it
                expandPercentage(genes.map { it.second }, targets, method, prob)
            }
        }

        val selected = selectWithSorted(group.subList(0, decideSize(group.size, percentage)), mutationSizeN)
        return selected //listOf(randomness.choose(selected))
    }

    /**
     * @param genes a map of genes with their corresponding impact
     * @param method a method to sort [genes]
     * @param targets applied targets to assess their impact
     */
    private fun <T> sortGenesBasedOnImpacts(genes: List<Pair<T, Impact>>, method: GeneMutationSelectionMethod, targets: Set<Int>): List<Pair<T, Impact>> {
        if (genes.isEmpty() || genes.size == 1) return genes
        return when (method) {
            GeneMutationSelectionMethod.AWAY_NOIMPACT -> sortListByImpact(genes, targets = targets, properties = arrayOf(ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET))
            GeneMutationSelectionMethod.APPROACH_IMPACT -> sortListByImpact(genes, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT))
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPACT -> sortListByImpact(genes, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT))
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPROVEMENT -> sortListByImpact(genes, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPROVEMENT))
            GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT -> sortListByImpact(genes, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET))
            else -> {
                genes
            }
        }
    }

    /**
     * ideally, candidates should shrink with search for a focused mutation.
     * but if there is no enough info for deciding whether the gene is impactful, we need expand the number of candidates to mutate.
     */
    private fun expandPercentage(impacts: List<Impact>, targets: Set<Int>, method: GeneMutationSelectionMethod, percentage: Double): Double {
        var modifyPercentage = percentage
        val properties = mutableSetOf<ImpactProperty>()
        when (method) {
            GeneMutationSelectionMethod.AWAY_NOIMPACT -> properties.add(ImpactProperty.TIMES_NO_IMPACT)
            GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT -> {
                properties.add(ImpactProperty.TIMES_NO_IMPACT)
                properties.add(ImpactProperty.TIMES_IMPACT)
            }
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPROVEMENT -> {
                properties.add(ImpactProperty.TIMES_IMPACT)
                properties.add(ImpactProperty.TIMES_CONS_NO_IMPROVEMENT)
            }
            GeneMutationSelectionMethod.APPROACH_IMPACT -> properties.add(ImpactProperty.TIMES_IMPACT)

            GeneMutationSelectionMethod.APPROACH_LATEST_IMPACT -> {
                properties.add(ImpactProperty.TIMES_IMPACT)
                properties.add(ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT)
            }
        }
        val rank = properties.map { ImpactUtils.getImpactDistribution(impacts, it, targets) }.sortedByDescending { it.rank }

        if (rank.first() == ImpactPropertyDistribution.NONE) modifyPercentage = 1.0
        if (rank.first() == ImpactPropertyDistribution.FEW && modifyPercentage < 0.7) modifyPercentage = 0.7

        return modifyPercentage
    }

    /**
     * decide an archive-based gene selection method when the selection is adaptive (i.e., [GeneMutationSelectionMethod.adaptive])
     */
    private fun decideArchiveGeneSelectionMethod(genes : List<Impact>) : GeneMutationSelectionMethod {
        return when (config.adaptiveGeneSelectionMethod) {
            GeneMutationSelectionMethod.ALL_FIXED_RAND -> randomGeneSelectionMethod()
            else -> config.adaptiveGeneSelectionMethod
        }
    }

    private fun randomGeneSelectionMethod(): GeneMutationSelectionMethod = randomness.choose(GeneMutationSelectionMethod.values().filter { !it.adaptive && it.archive })

    /**
     * this fun is used by [Archive] to sample an individual from population (i.e., [individuals])
     * if [ArchiveMutator.enableArchiveSelection] is true.
     * In order to identify impacts of genes, we prefer to select an individual which has some impact info.
     */
    fun <T : Individual> selectIndividual(individuals: List<EvaluatedIndividual<T>>): EvaluatedIndividual<T> {
        if (randomness.nextBoolean(0.1)) return randomness.choose(individuals)
        val impacts = individuals.filter { it.anyImpactfulGene() }
        if (impacts.isNotEmpty()) return randomness.choose(impacts)
        return randomness.choose(individuals)
    }

    private fun <T> prioritizeNoVisit(genes: List<Pair<T, Impact>>): List<Pair<T, Impact>> {
        val noVisit = genes.filter { it.second.getTimesToManipulate() == 0 }.toMutableList()
        noVisit.shuffle()
        return noVisit
    }

    private fun <T> sortListByImpact(list: List<Pair<T, Impact>>, targets: Set<Int>, properties: Array<ImpactProperty>): List<Pair<T, Impact>> {
        val sorted = sortImpact(list.map { it.second }, targets, properties)
        return list.sortedBy { g ->
            sorted.map { it.indexOf(g.second) }.sum()
        }

    }

    private fun sortImpact(allImpacts : List<Impact>, targets: Set<Int>, properties: Array<ImpactProperty>) : List<List<Impact>>{
        return if (config.sortedByCounter) {
            properties.map { property ->
                when (property) {
                    ImpactProperty.TIMES_IMPACT -> {
                        allImpacts.map { Pair(it, getCounterByProperty(it, property, targets)) }.sortedByDescending { it.second }.map { it.first }
                    }
                    else -> {
                        allImpacts.map { Pair(it, getCounterByProperty(it, property, targets)) }.sortedBy { it.second }.map { it.first }
                    }
                }
            }
        } else {
            properties.map { property ->
                when (property) {
                    ImpactProperty.TIMES_IMPACT -> {
                        allImpacts.map { Pair(it, getDegreeByProperty(it, property, targets)) }.sortedByDescending { it.second }.map { it.first }
                    }
                    else -> {
                        allImpacts.map { Pair(it, getDegreeByProperty(it, property, targets)) }.sortedBy { it.second }.map { it.first }
                    }
                }
            }
        }
    }


    private fun getCounterByProperty(impact: Impact, property: ImpactProperty, targets: Set<Int>): Int {
        val value = when (property) {
            ImpactProperty.TIMES_IMPACT -> impact.getCounter(property, targets, By.MAX)
            else -> impact.getCounter(property, targets, By.MIN)
        }
        if (value < 0) return randomness.nextInt(0, impact.getTimesToManipulate())
        return value
    }


    private fun getDegreeByProperty(impact: Impact, property: ImpactProperty, targets: Set<Int>): Double {
        val value = when (property) {
            ImpactProperty.TIMES_IMPACT -> impact.getDegree(property, targets, By.MAX)
            else -> impact.getDegree(property, targets, By.MIN)
        }
        if (value < 0) return randomness.nextDouble()
        return value
    }


    private fun decideSize(list: Int, percentage: Double) = max(1, list * percentage.toInt())

    /**************************** gene mutation ********************************************/

    fun withinNormal(prob: Double = WITHIN_NORMAL): Boolean {
        return randomness.nextBoolean(prob)
    }

    fun mutate(gene: Gene) {
        val p = gene.copy()
        when (gene) {
            is StringGene -> mutate(gene)
//            is IntegerGene -> mutate(gene)
//            is EnumGene<*> -> mutate(gene)
            else -> {
                if (ParamUtil.getValueGene(gene) is StringGene) {
                    mutate(ParamUtil.getValueGene(gene))
                } else {
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
    private fun mutate(gene: StringGene, probOfModifyChar: Double = 0.8, priorLengthMutation: Boolean = false) {
        /**
         * init charsMutation
         */
        if (gene.mutatedIndex == -2) {
            if (gene.charsMutation.isNotEmpty()) {
                log.warn("duplicated Initialized")
                if (gene.charsMutation.size != gene.value.length) {
                    gene.charsMutation.clear()
                    gene.charsMutation.addAll((0 until gene.value.length).map { createCharMutationUpdate() })
                }
            } else
                gene.charsMutation.addAll((0 until gene.value.length).map { createCharMutationUpdate() })

            gene.charMutationInitialized()
        }

        /*
        if value is blank, prefer appending a new string
         */
        if (gene.value.isBlank()) {
            append(gene, CharPool.WORD, modifyCharMutation = true)
            return
        }

        if (priorLengthMutation) {
            if (!gene.lengthMutation.reached) {
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

        if (doCharMutation == doLenMutation) {
            if (randomness.nextBoolean())
                doCharMutation = !doCharMutation
            else
                doLenMutation = !doLenMutation
        }

        if (doCharMutation) {
            val index = decideIndex(gene)
            gene.mutatedIndex = index
            modify(gene, index, gene.charsMutation[index])
        } else if (doLenMutation) {
            val exclude = gene.charsMutation.mapIndexed { index, intMutationUpdate -> if (intMutationUpdate.reached && randomness.nextBoolean(WITHIN_NORMAL)) index else -1 }.filter { it > -1 }
            val last = if (exclude.isNotEmpty()) exclude.max()!! else -1
            modifyLength(gene, last, modifyCharMutation = true)
        } else
            log.warn("at least one of doCharMutation {} and doLenMutation {} should be enabled", doCharMutation, doLenMutation)
    }

    private fun approachPrefer(gene: StringGene): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> withinNormal()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> withinNormal(gene.dependencyInfo.degreeOfIndependence)
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    private fun approachSlightMutation(gene: StringGene): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            /*
            if Independence is higher, far away from approachSlightMutation
            if Independence is lower, close to approachSlightMutation
            */
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> gene.dependencyInfo.resetTimes > DEP_THRESHOLD && withinNormal()
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalStateException("bug!")
        }
    }

    private fun approachSlightMutation(info: GeneIndependenceInfo): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> info.resetTimes > 0 && withinNormal()
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    /**
     * one adaptive point
     */
    private fun decideIndex(gene: StringGene): Int {
        if (approachPrefer(gene)) {
            /**
             * first index of char that has not reached optima yet
             */
            gene.charsMutation.indexOfFirst {
                !it.reached
            }.let {
                if (it != -1) return it
            }
        }
        log.trace("Deciding index")
        return randomness.nextInt(gene.value.length)
    }

    private fun modifyLength(gene: StringGene, strictAfter: Int, modifyCharMutation: Boolean) {
        val current = gene.value.length
        val min = if (strictAfter > gene.lengthMutation.preferMin && strictAfter <= gene.lengthMutation.preferMax) strictAfter else gene.lengthMutation.preferMin
        val validCandidates = validateCandidates(min, gene.lengthMutation.preferMax, listOf(current))

        val p = randomness.nextBoolean()
        if (validCandidates == 0) {
            when {
                gene.value.length == gene.maxLength || !p -> {
                    delete(gene, modifyCharMutation = modifyCharMutation)
                }
                gene.value.isBlank() || p -> {
                    append(gene, CharPool.WORD, modifyCharMutation = modifyCharMutation)
                }
            }
        } else {
            val normal = approachPrefer(gene)//withinNormal(gene.degreeOfIndependency)
            when {
                current == gene.maxLength || (current == gene.lengthMutation.preferMax && normal) || !p -> {
                    delete(gene, modifyCharMutation = modifyCharMutation)
                }
                else -> {
                    val start = (if (!normal || current > gene.lengthMutation.preferMax) gene.maxLength - current else gene.lengthMutation.preferMax - current)
                    val delta = apc.getExploratoryValue(start = if (start > MAX_STRING_LEN_MUTATION) MAX_STRING_LEN_MUTATION else start, end = 1)
                    append(gene, CharPool.WORD, num = delta, modifyCharMutation = modifyCharMutation)
                }
            }
        }

        if (current == gene.value.length || (gene.value.length < gene.minLength && gene.value.length > gene.maxLength))
            log.warn("length of value of string gene should be changed after length mutation: previous {} vs. current {}", current, gene.value.length)
    }

    private fun modify(gene: StringGene, index: Int, charMutation: IntMutationUpdate) {
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
            1 -> gene.value = modifyIndex(gene.value, gene.mutatedIndex, (charMutation.preferMin..charMutation.preferMax).toMutableList().first { it != current }.toChar())
            else -> {
                val char =
                        if (approachSlightMutation(gene)) //prefer middle if the degree of independent is quite high
                            randomFromCurrentAdaptively(
                                    current,
                                    charMutation.preferMin,
                                    charMutation.preferMax,
                                    Char.MIN_VALUE.toInt(),
                                    Char.MAX_VALUE.toInt(),
                                    start = 6,
                                    end = 3).toChar()
                        else
                            preferMiddle(charMutation.preferMin, charMutation.preferMax, current).toChar()

                gene.value = modifyIndex(gene.value, gene.mutatedIndex, char = char)
            }
        }
        if (current == gene.value.toCharArray()[index].toInt())
            log.warn("char should be modified after char mutation: previous {} vs. current {}", current, gene.value.toCharArray()[index].toInt())
    }

    private fun preferMiddle(min: Int, max: Int, current: Int): Int {
        if (min > max)
            log.warn("min {} should not be more than max {}", min, max)
        val cand = (if (current == min) ((min + 1)..max)
        else if (current == max) min until max
        else if (current in min..max) (min until current).plus((current + 1)..max)
        else min..max).toList()

        return if (withinNormal()) cand[cand.size / 2] else randomness.choose(cand)
    }

    /**
     * during later phase of search, modify char/int with relative close genes
     */
    private fun randomFromCurrentAdaptively(current: Int, minValue: Int, maxValue: Int, hardMinValue: Int, hardMaxValue: Int, start: Int, end: Int): Int {
        val prefer = withinNormal()
        val range = max(abs((if (prefer) maxValue else hardMaxValue) - current), abs((if (prefer) minValue else hardMinValue) - current)).toLong()
        val delta = GeneUtils.getDelta(randomness, apc, range = range, start = start, end = end)
        val value = if (prefer && current + delta > maxValue)
            current - delta
        else if (prefer && current - delta < minValue)
            current + delta
        else
            current + delta * randomness.choose(listOf(-1, 1))

        return when {
            value < hardMinValue -> if (current == hardMinValue) hardMinValue + 1 else hardMinValue
            value > hardMaxValue -> if (current == hardMaxValue) hardMaxValue - 1 else hardMaxValue
            else -> value
        }
    }

    private fun delete(gene: StringGene, num: Int = 1, modifyCharMutation: Boolean) {
        if (modifyCharMutation && gene.value.length != gene.charsMutation.size) {
            log.warn("regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", gene.value.length, gene.value, gene.charsMutation.size)
        }
        val value = gene.value
        val expected = value.length - num
        gene.value = value.dropLast(num)

        if (modifyCharMutation) {
            if (num == 0)
                log.warn("mutated length of the gene should be more than 0")
            (0 until num).forEach { _ ->
                gene.charsMutation.removeAt(expected)
            }
        }
        if (modifyCharMutation && gene.value.length != gene.charsMutation.size) {
            log.warn("{} are deleted:regarding string gene, a length {} of a value {} of the gene should be always same with a size {} of its charMutation", num, gene.value.length, gene.value, gene.charsMutation.size)
        }
    }

    private fun append(gene: StringGene, charPool: CharPool, num: Int = 1, modifyCharMutation: Boolean) {
        if (modifyCharMutation && gene.value.length != gene.charsMutation.size) {
            log.warn("regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", gene.value.length, gene.value, gene.charsMutation.size)
        }

        if (num == 0)
            log.warn("mutated length of the gene should be more than 0")

        gene.value += String((0 until num).map { randomness.nextWordChar() }.toCharArray())
        if (modifyCharMutation)
            gene.charsMutation.addAll((0 until num).map { createCharMutationUpdate() })

        if (modifyCharMutation && gene.value.length != gene.charsMutation.size) {
            log.warn("{} are appended:regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", num, gene.value.length, gene.value, gene.charsMutation.size)
        }
    }

    private fun modifyIndex(value: String, index: Int, char: Char): String {
        if (index >= value.length) throw IllegalArgumentException("$index exceeds the length of $value")
        return value.toCharArray().also { it[index] = char }.joinToString("")
    }

    /**
     * [min] and [max] are inclusive
     */
    fun validateCandidates(min: Int, max: Int, exclude: List<Int>): Int {
        if (max < min)
            return 0
        if (max == min && exclude.contains(min)) return 0
        if (max == min)
            return 1
        return max - min + 1 - exclude.filter { it >= min || it <= max }.size
    }

    /**************************** utilities ********************************************/

    fun applyArchiveSelection() = enableArchiveSelection()
            && time.percentageUsedBudget() > config.startArchiveMutation //fixed time to start archive-based mio or based on how many impacts have been collected?
            && randomness.nextBoolean(config.probOfArchiveMutation)

    fun disableArchiveSelectionForGene() = !config.enableGeneSelectionMethodForGene

    fun enableArchiveSelection() = config.adaptiveGeneSelectionMethod != GeneMutationSelectionMethod.NONE && config.probOfArchiveMutation > 0.0

    fun enableArchiveGeneMutation() = config.probOfArchiveMutation > 0 && config.archiveGeneMutation != EMConfig.ArchiveGeneMutation.NONE

    fun enableArchiveMutation() = enableArchiveGeneMutation() || enableArchiveSelection()

    fun createCharMutationUpdate() = IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt())

    /**
     * export impact info collected during search that is normally used for experiment
     */
    fun exportImpacts(solution: Solution<*>) {
        val path = Paths.get(config.impactFile)
        if (path.parent != null) Files.createDirectories(path.parent)

        val content = mutableListOf<String>()
        content.add(mutableListOf("test", "actionIndex", "rootGene").plus(Impact.toCSVHeader()).joinToString(","))
        solution.individuals.forEachIndexed { index, e ->
            e.getInitializationGeneImpact().forEachIndexed { aindex, mutableMap ->
                mutableMap.forEach { t, geneImpact ->
                    content.add(mutableListOf(index.toString(), "Initialization$aindex", t).plus(geneImpact.toCSVCell()).joinToString(","))
                    geneImpact.flatViewInnerImpact().forEach { (name, impact) ->
                        content.add(mutableListOf(index.toString(), "Initialization$aindex", "$t-$name").plus(impact.toCSVCell()).joinToString(","))
                    }
                }
            }

            e.getActionGeneImpact().forEachIndexed { aindex, mutableMap ->
                mutableMap.forEach { t, geneImpact ->
                    content.add(mutableListOf(index.toString(), "Action$aindex", t).plus(geneImpact.toCSVCell()).joinToString(","))
                    geneImpact.flatViewInnerImpact().forEach { (name, impact) ->
                        content.add(mutableListOf(index.toString(), "Action$aindex", "$t-$name").plus(impact.toCSVCell()).joinToString(","))
                    }
                }
            }

        }
        if (content.size > 1) {
            Files.write(path, content)
        }
    }

    fun saveMutatedGene(mutatedGenes: MutatedGeneSpecification?, individual: Individual,index : Int, improve:Boolean){
        mutatedGenes?:return
        if(!config.saveMutatedGene) return

        val path = Paths.get(config.saveMutatedGeneFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)

        val content = mutableListOf<String>()
        content.addAll(mutatedGenes.mutatedGenes.mapIndexed { gindex, gene -> listOf(
                index,
                improve,
                gene.name,
                mutatedGenes.mutatedPosition[gindex],
                individual.seeActions()[mutatedGenes.mutatedPosition[gindex]].getName()).joinToString(",")})

        content.addAll(mutatedGenes.mutatedDbGenes.mapIndexed { gindex, gene -> listOf(
                index,
                improve,
                gene.name,
                mutatedGenes.mutatedDbActionPosition[gindex],
                individual.seeInitializingActions()[mutatedGenes.mutatedDbActionPosition[gindex]].getName()).joinToString(",")})

        if (content.isNotEmpty()) Files.write(path, content, StandardOpenOption.APPEND)
    }
}

enum class CharPool {
    ALL,
    WORD
}