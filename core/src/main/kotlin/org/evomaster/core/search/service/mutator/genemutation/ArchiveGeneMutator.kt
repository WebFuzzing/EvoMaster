package org.evomaster.core.search.service.mutator.genemutation

import com.google.inject.Inject
import org.apache.commons.lang3.mutable.Mutable
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.archive.ArchiveMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.archive.IntegerGeneArchiveMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.archive.StringGeneArchiveMutationInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * created by manzh on 2020-07-01
 */

class ArchiveGeneMutator{

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    @Inject
    lateinit var apc: AdaptiveParameterControl

    @Inject
    lateinit var mwc : MutationWeightControl

    @Inject
    lateinit var ags : ArchiveGeneSelector


    companion object{
        private val log: Logger = LoggerFactory.getLogger(ArchiveGeneMutator::class.java)

        const val DEP_THRESHOLD = 0

        const val PROB_MUTATE_CHAR = 0.8

        /**
         * control maximum length mutation for a string, otherwise, it is quite expensive.
         */
        const val MAX_STRING_LEN_MUTATION = 64
    }

    fun applyException() = randomness.nextBoolean(0.1)

    /**
     * identify mutationInfo for [gene] with specified [targets]
     */
    fun identifyMutation(gene: Gene, targets: Set<Int>) : ArchiveMutationInfo {
        return when (gene) {
            is StringGene -> gene.mutationInfo.sort(targets).lastOrNull() as? StringGeneArchiveMutationInfo ?: StringGeneArchiveMutationInfo(gene, this)
            is IntegerGene -> gene.mutationInfo.sort(targets).lastOrNull() as? IntegerGeneArchiveMutationInfo ?: IntegerGeneArchiveMutationInfo(gene)
            else -> throw IllegalArgumentException("don't support to get archiveMutationInfo for the gene")
        }
    }

    /**
     * apply archiveGeneMutator for [gene] regarding specified [targets]
     */
    fun mutate(gene: Gene, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, additionalGeneMutationInfo: AdditionalGeneSelectionInfo) {
        val employedTargets = additionalGeneMutationInfo.targets.filter {!IdMapper.isLocal(it)}.toSet()
        val p = gene.copy()

        when (gene) {
            is StringGene -> mutateString(gene, employedTargets, allGenes, selectionStrategy, additionalGeneMutationInfo)
            is IntegerGene -> mutateInteger(gene, employedTargets)
//            is EnumGene<*> -> mutate(gene)
            else -> {
                val g = ParamUtil.getValueGene(gene)
                if (g is StringGene) {
                    mutateStringValue(g, probOfModifyingChar = probabilityToMutateChar(g), priorLengthMutation = priorLengthMutation(g), targets = employedTargets)
                } else if(g is IntegerGene){
                    mutateInteger(g, employedTargets)
                } else {
                    log.warn("not implemented error")
                }
            }
        }
        if (p.containsSameValueAs(gene))
            log.warn("value of gene shouldn't be same with previous")
    }

    /**************************** Integer Gene ********************************************/
    private fun mutateInteger( gene : IntegerGene, targets: Set<Int>){

        // identify ArchiveMutationInfo
        val mutationInfo = identifyMutation(gene, targets)

        mutationInfo as? IntegerGeneArchiveMutationInfo?: throw IllegalStateException("mismatched ArchiveGeneInfo for IntegerGene")
        val new = if (approachPrefer(mutationInfo) && mutationInfo.valueMutation.preferMin != mutationInfo.valueMutation.preferMax){
            preferMiddle(min = mutationInfo.valueMutation.preferMin, max = mutationInfo.valueMutation.preferMax, current = gene.value)
        }else{
            randomFromCurrentAdaptively(
                    current = gene.value,
                    minValue = mutationInfo.valueMutation.preferMin,
                    maxValue = mutationInfo.valueMutation.preferMax,
                    hardMinValue = gene.min,
                    hardMaxValue = gene.max,
                    start = 6,
                    end = 3)
        }

        gene.value = new
    }


    /**************************** String Gene ********************************************/
    /**
     * adaptive mutate string gene with specialization
     */
    private fun mutateString(gene: StringGene, targets: Set<Int>, allGenes : List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, additionalGeneMutationInfo: AdditionalGeneSelectionInfo){
        (additionalGeneMutationInfo.impact as? StringGeneImpact)?: throw IllegalArgumentException("mismatched GeneImpact for StringGene")
        val impact = additionalGeneMutationInfo.impact

        val preferSpec = gene.specializationGenes.isNotEmpty()
        val specializationGene = gene.getSpecializationGene()

        val employSpec = doEmploy(impact = impact.employSpecialization, targets = targets)
        val employBinding = doEmploy(impact = impact.employBinding, targets = targets)
        if (preferSpec && employSpec){
            val selected = selectSpec(gene, impact, targets)
            if (specializationGene == null){
                gene.selectedSpecialization = selected
            }else {
                val currentImpact = impact.specializationGeneImpact[gene.selectedSpecialization]
                if (selected == gene.selectedSpecialization || currentImpact.recentImprovement()){
                    specializationGene.standardMutation(
                            randomness, apc, mwc, allGenes,selectionStrategy, true, additionalGeneMutationInfo.copyFoInnerGene(currentImpact as GeneImpact)
                    )
                }else{
                    gene.selectedSpecialization = selected
                }
            }
            if (employBinding) gene.handleBinding(allGenes = allGenes)
            return
        }else if (specializationGene != null){
            gene.selectedSpecialization = -1
            if (employBinding) gene.handleBinding(allGenes = allGenes)
            return
        }
        if (gene.redoTaint(apc, randomness, allGenes)) return

        mutateStringValue(gene, probOfModifyingChar = probabilityToMutateChar(gene), priorLengthMutation = priorLengthMutation(gene), targets = targets)
        gene.repair()
        if (employBinding) gene.handleBinding(allGenes = allGenes)
    }

    private fun doEmploy(impact: BinaryGeneImpact, targets: Set<Int>) : Boolean{
        val list =  listOf(true, false)
        val weights = ags.impactBasedOnWeights(
                impacts = listOf(impact.trueValue, impact.falseValue),
                targets = targets)


        val employSpec = mwc.selectSubsetWithWeight(weights = weights.mapIndexed { index, w ->  list[index] to w}.toMap(), forceNotEmpty = true, numToMutate = 1.0)
        return randomness.choose(employSpec)
    }

    private fun selectSpec(gene: StringGene, impact: StringGeneImpact, targets: Set<Int>) : Int{
        if (impact.specializationGeneImpact.size != gene.specializationGenes.size){
            log.warn("mismatched specialization impacts")
        }
        val weights = ags.impactBasedOnWeights(impacts = impact.specializationGeneImpact.subList(0, gene.specializationGenes.size), targets = targets)

        val selected = mwc.selectSubsetWithWeight(
                weights = weights.mapIndexed { index, d -> index to d }.toMap(),
                forceNotEmpty = true,
                numToMutate = 1.0
        )
        return randomness.choose(selected)
    }

    /**
     * mutate StringGene [gene] using archive-based method
     *
     * after sampling, it is likely that a length of the [gene] is close to optima. Thus, mutate length with a relative low probability.
     * consequently, we set a relative high value for [probOfModifyingChar], i.e., default is 0.8.
     * regarding [priorLengthMutation], it might achieve a worse performance when [gene] is related to other gene,
     * e.g., fitness is about how the [gene] is close to other [Gene]. so we disable it by default.
     */
    private fun mutateStringValue(gene: StringGene, probOfModifyingChar: Double, priorLengthMutation: Boolean, targets: Set<Int>) {
        // identify ArchiveMutationInfo
        val mutationInfo = identifyMutation(gene, targets)

        mutationInfo as? StringGeneArchiveMutationInfo ?: throw IllegalArgumentException("mismatched mutationInfo for StringGene")

        //if value is blank, prefer appending a new string
        if (gene.value.isBlank()) {
            append(gene, mutationInfo= mutationInfo, charPool = getCharPool(), modifyCharMutation = false)
            return
        }
        /*
            if enable priorLengthMutation
            char mutation would be applied until length mutation reaches its optimal
         */
        if (priorLengthMutation) {
            if (!mutationInfo.lengthMutation.reached) {
                modifyLength(gene, mutationInfo=mutationInfo, strictAfter = -1, modifyCharMutation = false)
                return
            }
        }

        val doCharMutation = isCharMutable(gene, mutationInfo = mutationInfo) && (mutationInfo.lengthMutation.reached || randomness.nextBoolean(probOfModifyingChar))

        if (doCharMutation) {
            val index = decideIndex(gene = gene, mutationInfo = mutationInfo)
            val charsMutation = if (index >= mutationInfo.charsMutation.size ) createCharMutationUpdate() else mutationInfo.charsMutation[index]

            modify(gene,  mutationInfo = mutationInfo, index = index, charMutation = charsMutation)
        } else {
            val exclude = mutationInfo.charsMutation.mapIndexed { index, intMutationUpdate -> if (intMutationUpdate.reached) index else -1 }.filter { it > -1 }
            //TODO
            val last = if (exclude.isNotEmpty()) exclude.max()!! else -1
            modifyLength(gene, mutationInfo = mutationInfo, strictAfter = last, modifyCharMutation = false)
        }
    }

    private fun isCharMutable(gene: StringGene, mutationInfo: StringGeneArchiveMutationInfo) : Boolean{
        val end = min(gene.value.length, mutationInfo.charsMutation.size)
        return mutationInfo.charsMutation.subList(0, end).any { !it.reached }
    }

    private fun approachPrefer(mutationInfo: ArchiveMutationInfo): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> !applyException()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> randomness.nextBoolean(mutationInfo.dependencyInfo.degreeOfIndependence)
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
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> mutationInfo.dependencyInfo.resetTimes > DEP_THRESHOLD
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalStateException("bug!")
        }
    }

    private fun approachSlightMutation(info: GeneIndependenceInfo): Boolean {
        return when (config.archiveGeneMutation) {
            EMConfig.ArchiveGeneMutation.SPECIFIED -> randomness.nextBoolean()
            EMConfig.ArchiveGeneMutation.ADAPTIVE -> info.resetTimes > 0
            EMConfig.ArchiveGeneMutation.NONE -> throw IllegalArgumentException("bug!")
        }
    }

    /**
     * one adaptive point
     */
    private fun decideIndex(gene :StringGene, mutationInfo: StringGeneArchiveMutationInfo): Int {
        val end = min(mutationInfo.charsMutation.size, gene.value.length)
        if (approachPrefer(mutationInfo)) {
            //first index of char that has not reached optima yet
            val first = mutationInfo.charsMutation.subList(0, end).indexOfFirst {
                !it.reached
            }
            return max(0, first)
        }
        return if (mutationInfo.charsMutation.isEmpty()) 0 else randomness.choose((0 until end).toList())
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
            val normal = approachPrefer(mutationInfo)
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
                gene.value = modifyIndex(gene.value, index, char = char.toChar())
            }
        }
        if (current == gene.value.toCharArray()[index].toInt())
            log.warn("char should be modified after char mutation: previous {} vs. current {}", current, gene.value.toCharArray()[index].toInt())
    }

    private fun preferMiddle(min: Int, max: Int, current: Int): Int {
        if (min > max)
            log.warn("min {} should not be more than max {}", min, max)
        val middle = listOf(min, max).average().toInt()
        Lazy.assert {
            middle in min..max
        }
        return when{
            middle != current -> middle
            middle < max -> middle + 1
            middle > min -> middle - 1
            else-> throw IllegalArgumentException("none to return with $min $max and $current")
        }
    }

    /**
     * during later phase of search, modify char/int with relative close genes
     */
    private fun randomFromCurrentAdaptively(current: Int, minValue: Int, maxValue: Int, hardMinValue: Int, hardMaxValue: Int, start: Int, end: Int): Int {
        val prefer = !applyException()//withinNormal()
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
        return max - min + 1 - exclude.filter { it in min..max }.size
    }


    fun <T: Individual> updateArchiveMutationInfo(trackedCurrent: EvaluatedIndividual<T>, trackedMutated: EvaluatedIndividual<T>, mutatedGenes: MutatedGeneSpecification, targetsEvaluated: Map<Int, EvaluatedMutation>){

        updateArchiveMutationInfoWithSpecificInfo(
                trackedCurrent = trackedCurrent,
                trackedMutated = trackedMutated,
                mutatedGenes = mutatedGenes.mutatedGeneInfo(),
                genesAtActionIndex = mutatedGenes.mutatedPosition,
                mutatedIndividual = mutatedGenes.mutatedIndividual!!,
                addedInitializingActions = mutatedGenes.addedInitializationGenes.isNotEmpty(),
                isSqlGene = false,
                targetsEvaluated = targetsEvaluated)


        updateArchiveMutationInfoWithSpecificInfo(
                trackedCurrent = trackedCurrent,
                trackedMutated = trackedMutated,
                mutatedGenes = mutatedGenes.mutatedDbGeneInfo(),
                genesAtActionIndex = mutatedGenes.mutatedDbActionPosition,
                mutatedIndividual = mutatedGenes.mutatedIndividual!!,
                addedInitializingActions = mutatedGenes.addedInitializationGenes.isNotEmpty(),
                isSqlGene = true,
                targetsEvaluated = targetsEvaluated)

    }

    /**
     * update mutation info based on archive
     * @param trackedCurrent is original eval. indi. before this mutation
     * @param trackedMutated is mutated individual with its fitness
     * @param mutatedGenes includes mutated genes
     * @param genesAtActionIndex indicates the position of genes at actions if there exists
     * @param mutatedIndividual is mutated individual
     * @param addedInitializingActions indicates whether initialization actions are newly added into this individual
     * @param isSqlGene indicates whether the mutated genes are from initialization actions
     * @param targetsEvaluated indicates results of mutation evaluation (i.e., compare mutated with current) for given targets
     */
    private fun <T: Individual> updateArchiveMutationInfoWithSpecificInfo(
            trackedCurrent: EvaluatedIndividual<T>,
            trackedMutated: EvaluatedIndividual<T>,
            mutatedGenes: List<Gene>,
            genesAtActionIndex: List<Int>,
            mutatedIndividual: Individual,
            addedInitializingActions: Boolean,
            isSqlGene: Boolean, targetsEvaluated: Map<Int, EvaluatedMutation>){

        Lazy.assert{
            mutatedGenes.size == genesAtActionIndex.size
        }
        /*
            TODO
            considering bindingIds of StringGene
         */
        mutatedGenes.forEachIndexed { index, s ->
            val id = ImpactUtils.generateGeneId(mutatedIndividual, s)
            val actionIndex = if (genesAtActionIndex.isNotEmpty()) genesAtActionIndex[index] else -1

            val mutatedGene = (trackedMutated.findGeneById(id, actionIndex, isDb = isSqlGene) ?: throw IllegalStateException("cannot find mutated (Sql-$isSqlGene) gene with id ($id) in current individual"))

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
               mutatedGene.archiveMutationUpdate(
                       original = previousValue,
                       mutated = s,
                       targetsEvaluated = targetsEvaluated.filter { it.key >=0 && it.value != EvaluatedMutation.EQUAL_WITH },
                       archiveMutator = this)
            }
        }

    }

    //FIXME MAN
    fun createCharMutationUpdate() = IntMutationUpdate(getDefaultCharMin(), getDefaultCharMax())

    fun initCharMutation(charsMutation: MutableList<IntMutationUpdate>, length: Int) {
        charsMutation.clear()
        charsMutation.addAll((0 until length).map {
            createCharMutationUpdate()
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
            throw IllegalArgumentException("invalid char")
        }
    }
    fun saveMutatedGene(mutatedGenes: MutatedGeneSpecification?, individual: Individual, index : Int, evaluatedMutation : EvaluatedMutation, targets: Set<Int>){
        ArchiveMutationUtils.saveMutatedGene(config, mutatedGenes, individual, index, evaluatedMutation, targets)
    }
}

enum class CharPool {
    ALL,
    WORD
}