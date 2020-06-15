package org.evomaster.core.search.service.mutator.geneMutation

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.Action
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
import org.evomaster.core.search.service.mutator.geneMutation.archive.ArchiveMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.archive.StringGeneArchiveMutationInfo
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

        const val PROB_MUTATE_CHAR = 0.8

        /**
         * control maximum length mutation for a string, otherwise, it is quite expensive.
         */
        const val MAX_STRING_LEN_MUTATION = 64
    }

    /**************************** gene selection ********************************************/

    /**
     * calculate weights of genes (ie, [map]) based on impacts
     */
    fun calculateWeightByArchive(genesToMutate : List<Gene>, map: MutableMap<Gene, Double> ,individual: Individual?, impacts: List<Impact>?, evi: EvaluatedIndividual<*>, targets : Set<Int>){

        val geneImpacts = if(individual != null) genesToMutate.map { g ->
            evi.getImpact(individual, g)
                    ?: throw IllegalArgumentException("mismatched gene and impact info during mutation")

        }else impacts?:throw IllegalArgumentException("missing impacts info")

        //TODO later adaptive decide selection method
        val method = decideArchiveGeneSelectionMethod()
        if (method.adaptive)
            throw IllegalArgumentException("the decided method should be a fixed method")

        val weights = impactBasedOnWeights(geneImpacts, method, targets).toMutableList()

        genesToMutate.forEachIndexed { index, gene ->
            map[gene] = weights[index]
        }
    }

    /**
     * @return weights calculated based on [impacts]
     * @param impacts a list of impacts
     * @param method a method to calculate weights of [impacts]
     * @param targets applied targets to assess their impact
     */
    private fun impactBasedOnWeights(impacts: List<Impact>, method: GeneMutationSelectionMethod, targets: Set<Int>): Array<Double> {
        return when (method) {
            GeneMutationSelectionMethod.AWAY_NOIMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET))
            GeneMutationSelectionMethod.APPROACH_IMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT))
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT))
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPROVEMENT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPROVEMENT))
            GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET))
            else -> {
                throw IllegalArgumentException("invalid gene selection method: method cannot be NONE or adaptive, but $method")
            }
        }
    }

    /**
     * ideally, candidates should shrink with search for a focused mutation.
     * but if there is no enough info for deciding whether the gene is impactful, we need expand budget to explore.
     */
    private fun expandBudgetToExplore(impacts: List<Impact>, targets: Set<Int>, method: GeneMutationSelectionMethod): Boolean {
        return getImpactPropertiesByMethod(method).map { ImpactUtils.getImpactDistribution(impacts, it, targets) }.any { it == ImpactPropertyDistribution.NONE || it == ImpactPropertyDistribution.FEW }
    }

    private fun getImpactPropertiesByMethod(method : GeneMutationSelectionMethod) : Array<ImpactProperty>{
        return when (method) {
            GeneMutationSelectionMethod.AWAY_NOIMPACT -> arrayOf(ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET)
            GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT -> {
                arrayOf(ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET, ImpactProperty.TIMES_IMPACT)
            }
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPROVEMENT -> {
                arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPROVEMENT)
            }
            GeneMutationSelectionMethod.APPROACH_IMPACT -> arrayOf(ImpactProperty.TIMES_IMPACT)

            GeneMutationSelectionMethod.APPROACH_LATEST_IMPACT -> {
                arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT)
            }else -> {
                throw IllegalArgumentException("invalid gene selection method: method cannot be NONE or adaptive, but $method")
            }
        }
    }


    /**
     * decide an archive-based gene selection method when the selection is adaptive (i.e., [GeneMutationSelectionMethod.adaptive])
     */
    private fun decideArchiveGeneSelectionMethod() : GeneMutationSelectionMethod {
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


    private fun impactBasedOnWeights(impacts : List<Impact>, targets: Set<Int>, properties: Array<ImpactProperty>) : Array<Double>{

        val values : List<MutableList<Double>> = impacts.map { impact ->
            properties.map { p->
                when(config.geneWeightBasedOnImpactsBy){
                    EMConfig.GeneWeightBasedOnImpact.COUNTER, EMConfig.GeneWeightBasedOnImpact.SORT_COUNTER -> getCounterByProperty(impact, p, targets).toDouble()
                    EMConfig.GeneWeightBasedOnImpact.RATIO, EMConfig.GeneWeightBasedOnImpact.SORT_RATIO -> getDegreeByProperty(impact, p, targets)
                }
            }.toMutableList()
        }

        handleNotVisit(values, properties.size)

        //Man: shall we normalize data?
        return when(config.geneWeightBasedOnImpactsBy){
            EMConfig.GeneWeightBasedOnImpact.COUNTER, EMConfig.GeneWeightBasedOnImpact.RATIO -> values.map { it.sum() + 1.0 }.toTypedArray()
            EMConfig.GeneWeightBasedOnImpact.SORT_COUNTER, EMConfig.GeneWeightBasedOnImpact.SORT_RATIO -> {
                val weights = Array(impacts.size){Array(properties.size){0.0}}
                (0 until properties.size).map {pi->
                    values.mapIndexed { index, list -> Pair(index, list[pi]) }.sortedBy { it.second }.forEachIndexed { index, pair ->
                        weights[pair.first][pi] = index + 1.0
                    }
                }
                weights.map { it.average() }.toTypedArray()
            }
        }
    }


    private fun handleNotVisit(values : List<MutableList<Double>>, sizeOfProperty: Int){

        (0 until sizeOfProperty).forEach {pi->
            val r = max(0.0,values.map { v->v[pi] }.max()?:0.0)
            values.forEachIndexed { index, list -> if (list[pi] < 0) values[index][pi] = r }
        }
    }

    /**
     * the more，the better
     */
    private fun getCounterByProperty(impact: Impact, property: ImpactProperty, targets: Set<Int>): Int {
        val value = impact.getCounter(property, targets, By.MAX)

        if (value < 0) return value

        return when (property) {
            ImpactProperty.TIMES_IMPACT -> value
            else -> impact.getTimesToManipulate() - value
        }
    }

    /**
     * the more，the better
     */
    private fun getDegreeByProperty(impact: Impact, property: ImpactProperty, targets: Set<Int>): Double {
        val value = impact.getDegree(property, targets, By.MAX)
        if (value < 0) return value

        return when (property) {
            ImpactProperty.TIMES_IMPACT -> value
            else -> 1.0 - value
        }
    }


    /**************************** gene mutation ********************************************/

    fun withinNormal(prob: Double = WITHIN_NORMAL): Boolean {
        return randomness.nextBoolean(prob)
    }

    fun mutate(gene: Gene, targets: Set<Int>) {
        val p = gene.copy()
        when (gene) {
            is StringGene -> mutate(gene, probOfModifyChar = probabilityToMutateChar(gene), priorLengthMutation = priorLengthMutation(gene), targets = targets)
//            is IntegerGene -> mutate(gene)
//            is EnumGene<*> -> mutate(gene)
            else -> {
                if (ParamUtil.getValueGene(gene) is StringGene) {
                    val g = ParamUtil.getValueGene(gene) as StringGene
                    mutate(g, probOfModifyChar = probabilityToMutateChar(g), priorLengthMutation = priorLengthMutation(g), targets = targets)
                } else {
                    log.warn("not implemented error")
                }
            }
        }
        if (p.containsSameValueAs(gene))
            log.warn("value of gene shouldn't be same with previous")
    }

    /**
     * mutate IntegerGene [gene] using archive-based method
     */
//    private fun mutate(gene: IntegerGene){
//       gene.value = randomFromCurrentAdaptively(gene.value, gene.archiveMutationInfo.valueMutation.preferMin, gene.archiveMutationInfo.valueMutation.preferMax, gene.min, gene.max, 6, 3)
//    }

    /**
     * mutate StringGene [gene] using archive-based method
     *
     * after sampling, it is likely that a length of the [gene] is close to optima. Thus, mutate length with a relative low probability.
     * consequently, we set a relative high value for [probOfModifyChar], i.e., default is 0.8.
     * regarding [priorLengthMutation], it might achieve a worse performance when [gene] is related to other gene,
     * e.g., fitness is about how the [gene] is close to other [Gene]. so we disable it by default.
     */
    private fun mutate(gene: StringGene, probOfModifyChar: Double, priorLengthMutation: Boolean, targets: Set<Int>) {
        // identify ArchiveMutationInfo
        val mutationInfo = gene.mutationInfo.sort(targets).firstOrNull() as? StringGeneArchiveMutationInfo ?: StringGeneArchiveMutationInfo(gene)

        /*
          init charsMutation
         */
        if (mutationInfo.neverArchiveMutate()) {
            if (mutationInfo.charsMutation.isNotEmpty()) {
                log.warn("duplicated Initialized")
                if (mutationInfo.charsMutation.size != gene.value.length) {
                    mutationInfo.charsMutation.clear()
                    mutationInfo.charsMutation.addAll((0 until gene.value.length).map { createCharMutationUpdate() })
                }
            } else
                mutationInfo.charsMutation.addAll((0 until gene.value.length).map { createCharMutationUpdate() })

            mutationInfo.charMutationInitialized()
        }

        /*
        if value is blank, prefer appending a new string
         */
        if (gene.value.isBlank()) {
            append(gene, mutationInfo= mutationInfo, charPool = getCharPool(), modifyCharMutation = true)
            return
        }

        if (priorLengthMutation) {
            if (!mutationInfo.lengthMutation.reached) {
                modifyLength(gene, mutationInfo=mutationInfo, strictAfter = -1, modifyCharMutation = true)
                return
            }
        }

        val p = withinNormal()

        val normalCharMutation = randomness.nextBoolean(probOfModifyChar)
        /**
         * with 10% probability, mutate char even all genes reach optima
         */
        var doCharMutation = if (mutationInfo.charsMutation.all { it.reached }) !p else normalCharMutation
        /**
         * with 10% probability, mutate length even current is optimal
         */
        var doLenMutation = if (mutationInfo.lengthMutation.reached) !p else !normalCharMutation

        if (doCharMutation == doLenMutation) {
            if (randomness.nextBoolean())
                doCharMutation = !doCharMutation
            else
                doLenMutation = !doLenMutation
        }

        if (doCharMutation) {
            val index = decideIndex(gene, mutationInfo = mutationInfo)
            mutationInfo.mutatedIndex = index
            modify(gene,  mutationInfo = mutationInfo,
                    index = index, charMutation = mutationInfo.charsMutation[index])
        } else if (doLenMutation) {
            val exclude = mutationInfo.charsMutation.mapIndexed { index, intMutationUpdate -> if (intMutationUpdate.reached && randomness.nextBoolean(WITHIN_NORMAL)) index else -1 }.filter { it > -1 }
            val last = if (exclude.isNotEmpty()) exclude.max()!! else -1
            modifyLength(gene, mutationInfo = mutationInfo, strictAfter = last, modifyCharMutation = true)
        } else
            log.warn("at least one of doCharMutation {} and doLenMutation {} should be enabled", doCharMutation, doLenMutation)
    }

    private fun approachPrefer(gene: StringGene, mutationInfo: ArchiveMutationInfo): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> withinNormal()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> withinNormal(mutationInfo.dependencyInfo.degreeOfIndependence)
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    private fun approachSlightMutation(gene: StringGene, mutationInfo: ArchiveMutationInfo): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            /*
            if Independence is higher, far away from approachSlightMutation
            if Independence is lower, close to approachSlightMutation
            */
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> mutationInfo.dependencyInfo.resetTimes > DEP_THRESHOLD && withinNormal()
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
    private fun decideIndex(gene: StringGene, mutationInfo: StringGeneArchiveMutationInfo): Int {
        if (approachPrefer(gene, mutationInfo)) {
            /*
              first index of char that has not reached optima yet
             */
            mutationInfo.charsMutation.indexOfFirst {
                !it.reached
            }.let {
                if (it != -1) return it
            }
        }
        log.trace("Deciding index")
        return randomness.nextInt(gene.value.length)
    }

    private fun modifyLength(gene: StringGene, mutationInfo: StringGeneArchiveMutationInfo, strictAfter: Int, modifyCharMutation: Boolean) {
        val current = gene.value.length
        val min = if (strictAfter > mutationInfo.lengthMutation.preferMin && strictAfter <= mutationInfo.lengthMutation.preferMax) strictAfter else mutationInfo.lengthMutation.preferMin
        val validCandidates = validateCandidates(min, mutationInfo.lengthMutation.preferMax, listOf(current))

        val p = randomness.nextBoolean()
        if (validCandidates == 0) {
            when {
                gene.value.length == gene.maxLength || !p -> {
                    delete(gene, mutationInfo = mutationInfo, modifyCharMutation = modifyCharMutation)
                }
                gene.value.isBlank() || p -> {
                    append(gene, mutationInfo = mutationInfo, charPool = getCharPool(), modifyCharMutation = modifyCharMutation)
                }
            }
        } else {
            val normal = approachPrefer(gene, mutationInfo)//withinNormal(gene.degreeOfIndependency)
            when {
                current == gene.maxLength || (current == mutationInfo.lengthMutation.preferMax && normal) || !p -> {
                    delete(gene, mutationInfo = mutationInfo, modifyCharMutation = modifyCharMutation)
                }
                else -> {
                    val start = (if (!normal || current > mutationInfo.lengthMutation.preferMax) gene.maxLength - current else mutationInfo.lengthMutation.preferMax - current)
                    val delta = apc.getExploratoryValue(start = if (start > MAX_STRING_LEN_MUTATION) MAX_STRING_LEN_MUTATION else start, end = 1)
                    append(gene, mutationInfo = mutationInfo, charPool =  getCharPool(), num = delta, modifyCharMutation = modifyCharMutation)
                }
            }
        }

        if (current == gene.value.length || (gene.value.length < gene.minLength && gene.value.length > gene.maxLength))
            log.warn("length of value of string gene should be changed after length mutation: previous {} vs. current {}", current, gene.value.length)
    }

    private fun modify(gene: StringGene, mutationInfo: StringGeneArchiveMutationInfo, index: Int, charMutation: IntMutationUpdate) {
        val current = gene.value.toCharArray()[index].toInt()

        when (validateCandidates(charMutation.preferMin, charMutation.preferMax, listOf(current))) {
            0 -> {
                if (!charMutation.reached)
                    log.warn("validCandidates can only be empty when selected is optimal")
                val char = randomFromCurrentAdaptively(
                        current,
                        getDefaultCharMin(),//Char.MIN_VALUE.toInt(),
                        getDefaultCharMax(),//Char.MAX_VALUE.toInt(),
                        getDefaultCharMin(),//Char.MIN_VALUE.toInt(),
                        getDefaultCharMax(),//Char.MAX_VALUE.toInt(),
                        start = 6,
                        end = 3)
                validateChar(char)
                gene.value = modifyIndex(gene.value, index, char = char.toChar())
            }
            1 -> gene.value = modifyIndex(gene.value, index, (charMutation.preferMin..charMutation.preferMax).toMutableList().first { it != current }.toChar())
            else -> {
                val char =
                        if (approachSlightMutation(gene, mutationInfo)) //prefer middle if the degree of independent is quite high
                            randomFromCurrentAdaptively(
                                    current,
                                    charMutation.preferMin,
                                    charMutation.preferMax,
                                    getDefaultCharMin(),//Char.MIN_VALUE.toInt(),
                                    getDefaultCharMax(),//Char.MAX_VALUE.toInt(),
                                    start = 6,
                                    end = 3)
                        else
                            preferMiddle(charMutation.preferMin, charMutation.preferMax, current)
                validateChar(char)
                gene.value = modifyIndex(gene.value, index, char = char.toChar())
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

    private fun delete(gene: StringGene, mutationInfo: StringGeneArchiveMutationInfo, num: Int = 1, modifyCharMutation: Boolean) {
        if (modifyCharMutation && gene.value.length != mutationInfo.charsMutation.size) {
            log.warn("regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", gene.value.length, gene.value, mutationInfo.charsMutation.size)
        }
        val value = gene.value
        val expected = value.length - num
        gene.value = value.dropLast(num)

        if (modifyCharMutation) {
            if (num == 0)
                log.warn("mutated length of the gene should be more than 0")
            (0 until num).forEach { _ ->
                mutationInfo.charsMutation.removeAt(expected)
            }
        }
        if (modifyCharMutation && gene.value.length != mutationInfo.charsMutation.size) {
            log.warn("{} are deleted:regarding string gene, a length {} of a value {} of the gene should be always same with a size {} of its charMutation", num, gene.value.length, gene.value, mutationInfo.charsMutation.size)
        }
    }

    private fun append(gene: StringGene, mutationInfo: StringGeneArchiveMutationInfo, charPool: CharPool, num: Int = 1, modifyCharMutation: Boolean) {
        if (modifyCharMutation && gene.value.length != mutationInfo.charsMutation.size) {
            log.warn("regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", gene.value.length, gene.value, mutationInfo.charsMutation.size)
        }

        if (num == 0)
            log.warn("mutated length of the gene should be more than 0")

        gene.value += String((0 until num).map {
            if (charPool == CharPool.WORD)
                randomness.nextWordChar()
            else
                randomness.nextChar()
        }.toCharArray())
        if (modifyCharMutation)
            mutationInfo.charsMutation.addAll((0 until num).map { createCharMutationUpdate() })

        if (modifyCharMutation && gene.value.length != mutationInfo.charsMutation.size) {
            log.warn("{} are appended:regarding string gene, a length {} of a value of the gene {} should be always same with a size {} of its charMutation", num, gene.value.length, gene.value, mutationInfo.charsMutation.size)
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


    fun <T: Individual> updateArchiveMutationInfo(trackedCurrent: EvaluatedIndividual<T>, current: EvaluatedIndividual<T>, mutatedGenes: MutatedGeneSpecification, targetsEvaluated: Map<Int, Int>){

        updateArchiveMutationInfoWithSpecificInfo(
                trackedCurrent,
                current,
                mutatedGenes = mutatedGenes.mutatedGenes,
                genesAtActionIndex = mutatedGenes.mutatedPosition,
                mutatedIndividual = mutatedGenes.mutatedIndividual!!,
                addedInitializingActions = mutatedGenes.addedInitializationGenes.isNotEmpty(),
                isSqlGene = false,
                targetsEvaluated = targetsEvaluated)


        updateArchiveMutationInfoWithSpecificInfo(
                trackedCurrent,
                current,
                mutatedGenes = mutatedGenes.mutatedDbGenes,
                genesAtActionIndex = mutatedGenes.mutatedDbActionPosition,
                mutatedIndividual = mutatedGenes.mutatedIndividual!!,
                addedInitializingActions = mutatedGenes.addedInitializationGenes.isNotEmpty(),
                isSqlGene = true,
                targetsEvaluated = targetsEvaluated)

    }

    private fun <T: Individual> updateArchiveMutationInfoWithSpecificInfo(
            trackedCurrent: EvaluatedIndividual<T>,
            current: EvaluatedIndividual<T>,
            mutatedGenes: List<Gene>,
            genesAtActionIndex:List<Int>,
            mutatedIndividual : Individual,
            addedInitializingActions: Boolean,
            isSqlGene: Boolean , targetsEvaluated: Map<Int, Int>){

        Lazy.assert{
            mutatedGenes.size == genesAtActionIndex.size
        }
        mutatedGenes.forEachIndexed { index, s ->
            val id = ImpactUtils.generateGeneId(mutatedIndividual, s)
            val actionIndex = if (genesAtActionIndex.isNotEmpty()) genesAtActionIndex[index] else -1

            val savedGene = (current.findGeneById(id, actionIndex, isDb = isSqlGene) ?: throw IllegalStateException("cannot find mutated (Sql-$isSqlGene) gene with id ($id) in current individual"))

            /*
                it may happen, i.e., a gene may be added during 'structureMutator.addInitializingActions(current, mutatedGenes)'
             */
            val previousValue = trackedCurrent.findGeneById(id, actionIndex, isDb = isSqlGene)

            if (previousValue == null){
                if (!isSqlGene)
                    throw IllegalStateException("cannot find mutated gene with id ($id) in its original individual")
                else{
                    /*
                        it may happen, i.e., a gene may be added during 'structureMutator.addInitializingActions(current, mutatedGenes)'
                     */
                    log.warn("cannot find gene{} at {} of (is newly added?{}) initializationActions", id, index, addedInitializingActions)
                }
            }else{
                savedGene.archiveMutationUpdate(original = previousValue, mutated = s, targetsEvaluated = targetsEvaluated.filterValues { it != 0 }, archiveMutator = this)
            }
        }

    }
    /**************************** utilities ********************************************/

    /**
     * @return whether apply archive-based gene selection for individual or eg, ObjectGene
     */
    fun applyArchiveSelection() = enableArchiveSelection()
            && time.percentageUsedBudget() > config.startArchiveMutation //fixed time to start archive-based mio or based on how many impacts have been collected?
            && randomness.nextBoolean(config.probOfArchiveMutation)


    /**
     * @return whether archive-based gene selection is enabled based on the configuration, ie, EMConfig
     */
    fun enableArchiveSelection() = config.adaptiveGeneSelectionMethod != GeneMutationSelectionMethod.NONE && doCollectImpact()

    /**
     * @return whether archive-based gene mutation is enabled based on the configuration, ie, EMConfig
     */
    fun enableArchiveGeneMutation() = doCollectImpact() && config.archiveGeneMutation != EMConfig.ArchiveGeneMutation.NONE

    /**
     * @return whether archive-based solution (ie, gene selection or gene mutation) is enabled based on the configuration, ie, EMConfig
     */
    fun enableArchiveMutation() = enableArchiveGeneMutation() || enableArchiveSelection()

    /**
     * @return whether collect impacts info after each mutation during search
     */
    fun doCollectImpact() = config.probOfArchiveMutation > 0.0

    //FIXME MAN
    fun createCharMutationUpdate() = IntMutationUpdate(getDefaultCharMin(), getDefaultCharMax())

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

    fun saveImpactSnapshot(index : Int, checkedTargets: Set<Int>, targetsInfo : MutableMap<Int, Int>, addedToArchive: Boolean, evaluatedIndividual: EvaluatedIndividual<*>) {

        if(config.saveImpactAfterMutationFile.isBlank()) return

        val path = Paths.get(config.saveImpactAfterMutationFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)
        val text = "$index,${checkedTargets.joinToString("-")},${targetsInfo.filterValues { it >=0 }.keys.joinToString("-")},${targetsInfo.filterValues { it == 1 }.keys.joinToString("-")},$addedToArchive,"
        val content = evaluatedIndividual.exportImpactAsListString().map { "$text,$it" }
        if (content.isNotEmpty()) Files.write(path, content, StandardOpenOption.APPEND)

    }

    fun saveMutatedGene(mutatedGenes: MutatedGeneSpecification?, individual: Individual,index : Int, improve:Boolean){
        mutatedGenes?:return
        if(config.saveMutatedGeneFile.isBlank()) return

        val path = Paths.get(config.saveMutatedGeneFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)

        val content = mutableListOf<String>()
        content.addAll(mutatedGenes.mutatedGenes.mapIndexed { gindex, gene -> listOf(
                index,
                improve,
                gene.name,
                gene.getValueAsPrintableString(),
                mutatedGenes.mutatedPosition[gindex],
                getActionInfo(individual.seeActions()[mutatedGenes.mutatedPosition[gindex]])).joinToString(",")})

        content.addAll(mutatedGenes.mutatedDbGenes.mapIndexed { gindex, gene -> listOf(
                index,
                improve,
                gene.name,
                gene.getValueAsPrintableString(),
                mutatedGenes.mutatedDbActionPosition[gindex],
                getActionInfo(individual.seeInitializingActions()[mutatedGenes.mutatedDbActionPosition[gindex]])).joinToString(",")})

        if (content.isNotEmpty()) {
            try {
                Files.write(path, content, StandardOpenOption.APPEND)
            }catch (e :java.nio.charset.MalformedInputException){
                print("")
            }
        }
    }

    private fun getActionInfo(action : Action) : String{
        return if (action is RestCallAction) action.resolvedPath()
        else action.getName()
    }

    fun initCharMutation(charsMutation: MutableList<IntMutationUpdate>, length: Int) {
        charsMutation.clear()
        charsMutation.addAll((0 until length).map {
            createCharMutationUpdate() //IntMutationUpdate(getDefaultCharMin(), getDefaultCharMax())
        })
    }

    private fun getCharPool() = CharPool.WORD

    fun getDefaultCharMin() = when(getCharPool()){
        CharPool.ALL -> Char.MIN_VALUE.toInt()
        CharPool.WORD -> randomness.wordCharPool().first()
    }

    fun getDefaultCharMax() = when(getCharPool()){
        CharPool.ALL -> Char.MAX_VALUE.toInt()
        CharPool.WORD -> randomness.wordCharPool().last()
    }

    private fun probabilityToMutateChar(gene: StringGene) : Double{
        return PROB_MUTATE_CHAR
    }

    private fun priorLengthMutation(gene: StringGene) = false//(!gene.archiveMutationInfo.lengthMutation.reached) && randomness.nextBoolean(0.9)

    private fun validateChar(char : Int) {
        val r = when(getCharPool()){
            CharPool.ALL -> char in Char.MIN_VALUE.toInt()..Char.MAX_VALUE.toInt()
            CharPool.WORD -> randomness.wordCharPool().contains(char)
        }
        if (!r){
            //throw IllegalArgumentException("invalid char")
            log.warn(char.toString())
        }
    }

    fun doApplyAGM(gene: Gene) : Boolean = gene is StringGene
}

enum class CharPool {
    ALL,
    WORD
}