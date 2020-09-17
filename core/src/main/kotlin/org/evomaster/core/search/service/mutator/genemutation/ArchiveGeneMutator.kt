package org.evomaster.core.search.service.mutator.genemutation

import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.mutationupdate.DoubleMutationUpdate
import org.evomaster.core.search.service.mutator.genemutation.mutationupdate.LongMutationUpdate
import org.evomaster.core.search.service.mutator.genemutation.mutationupdate.MutationBoundaryUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    }

    private fun manageHistory(additionalGeneMutationInfo: AdditionalGeneMutationInfo, targets: Set<Int>) : List<Pair<Gene, EvaluatedInfo>> {
        return when(config.archiveGeneMutation){
            EMConfig.ArchiveGeneMutation.SPECIFIED_WITH_TARGETS -> additionalGeneMutationInfo.history.filter { it.second.targets.any { t-> targets.contains(t) } && it.second.result?.isImpactful()?:true }
            EMConfig.ArchiveGeneMutation.SPECIFIED_WITH_SPECIFIC_TARGETS -> additionalGeneMutationInfo.history.filter { it.second.specificTargets.any { t-> targets.contains(t) } && it.second.result?.isImpactful()?:true }
            else -> additionalGeneMutationInfo.history
        }
    }

    fun historyBasedValueMutation(additionalGeneMutationInfo: AdditionalGeneMutationInfo, gene: Gene, allGenes: List<Gene>) {
        val history = manageHistory(additionalGeneMutationInfo, additionalGeneMutationInfo.targets)
        when (gene) {
            is StringGene -> {
                val applied = deriveMutatorForStringValue(history, gene, allGenes)
                if (!applied) gene.standardValueMutation(randomness, allGenes, apc)
            }
            is IntegerGene -> gene.value = sampleValue(
                    history = history.map {
                        ((it.first as? IntegerGene)
                                ?: throw DifferentGeneInHistory(gene, it.first)
                                ).value.toLong() to (it.second.result?.isEffective() == true)
                    },
                    value = gene.value.toLong(),
                    valueUpdate = LongMutationUpdate(min = gene.min.toLong(), max = gene.max.toLong()),
                    start = GeneUtils.intpow2.size, end = 10
            ).toInt()
            is LongGene -> gene.value =  sampleValue(
                    history = history.map {
                        ((it.first as? LongGene)
                                ?: throw DifferentGeneInHistory(gene, it.first)).value to (it.second.result?.isEffective() == true)
                    },
                    value = gene.value,
                    valueUpdate = LongMutationUpdate(min = Long.MIN_VALUE, max = Long.MAX_VALUE),
                    start = GeneUtils.intpow2.size, end = 10
            )
            is DoubleGene -> gene.value =  sampleValue(
                    history = history.map {
                        ((it.first as? DoubleGene)?: throw DifferentGeneInHistory(gene, it.first)).value to (it.second.result?.isEffective() == true)
                    },
                    value = gene.value,
                    valueUpdate = DoubleMutationUpdate(min = Double.MIN_VALUE, max = Double.MAX_VALUE),
                    start = GeneUtils.intpow2.size, end = 10
            )
            is FloatGene -> gene.value = sampleValue(
                    history = history.map {
                        ((it.first as? FloatGene)?: throw DifferentGeneInHistory(gene, it.first)).value.toDouble() to (it.second.result?.isEffective() == true)
                    },
                    value = gene.value.toDouble(),
                    valueUpdate = DoubleMutationUpdate(min = Float.MIN_VALUE.toDouble(), max = Float.MAX_VALUE.toDouble()),
                    start = GeneUtils.intpow2.size, end = 10
            ).toFloat()
            else -> throw IllegalArgumentException("history-based value mutation is not applicable for ${gene::class.java.simpleName}")
        }
    }

    private fun probOfMiddle(update : MutationBoundaryUpdate<*>) : Double{
        return when {
            update.counter > 3 || update.updateTimes < 3 -> apc.getExploratoryValue(0.8, 0.5)
            update.counter > 1 || update.updateTimes in 3..5 -> apc.getExploratoryValue(0.5, 0.1)
            else -> 0.1
        }
    }

    private fun<T: Number> sampleValue(history : List<Pair<T, Boolean>>, value: T, valueUpdate: MutationBoundaryUpdate<T>, start: Int, end: Int) : T {
        (0 until history.size).forEach {i->
            valueUpdate.updateOrRestBoundary(
                    current = history[i].first,
                    doesCurrentBetter = history[i].second
            )
        }
        return valueUpdate.random(
                randomness = randomness,
                apc = apc,
                current = value,
                probOfMiddle = probOfMiddle(valueUpdate),
                start = start,
                end = end,
                minimalTimeForUpdate = 5
        )
    }

    /**************************** String Gene ********************************************/

    private fun deriveMutatorForStringValue(history : List<Pair<Gene, EvaluatedInfo>>, gene: StringGene, allGenes: List<Gene>) : Boolean{
        val others = allGenes.flatMap { it.flatView() }
                .filterIsInstance<StringGene>()
                .map { it.getValueAsRawString() }
                .filter { it != gene.value }
                .filter { !TaintInputName.isTaintInput(it) }

        val lenMutationUpdate = LongMutationUpdate(gene.minLength.toLong(), gene.maxLength.toLong())
        val charsMutationUpdate = (0 until gene.value.length).map { createCharMutationUpdate() }
        (0 until history.size).forEach { i ->
            val c = ( history[i].first as? StringGene)?.value ?: throw IllegalStateException("invalid extracted history element")
            val better = history[i].second.result?.isEffective() == true

            lenMutationUpdate.updateOrRestBoundary(
                    current = c.length.toLong(),
                    doesCurrentBetter = better
            )
            charsMutationUpdate.forEachIndexed { index, intMutationUpdate ->
                if (c.elementAtOrNull(index) != null){
                    intMutationUpdate.updateOrRestBoundary(
                            current = c.elementAt(index).toLong(),
                            doesCurrentBetter = better
                    )
                }
            }
        }

        val p = randomness.nextDouble()
        val pOfLen = apc.getExploratoryValue(0.6, 0.2)
        val pLength = lenMutationUpdate.random(
                apc = apc,
                randomness = randomness,
                current = gene.value.length.toLong(),
                probOfMiddle = probOfMiddle(lenMutationUpdate),
                start = 6,
                end = 3,
                minimalTimeForUpdate = 2
        )

        val anyCharToMutate = charsMutationUpdate.filterIndexed {
            index, longMutationUpdate -> !longMutationUpdate.isReached(gene.value[index].toLong()) }.isNotEmpty() && charsMutationUpdate.isNotEmpty()
        if (!anyCharToMutate && lenMutationUpdate.isReached(gene.value.length.toLong())) return false

        if (p < 0.02 && others.isNotEmpty()){
            gene.value = randomness.choose(others)
            return true
        }

        if (lenMutationUpdate.isReached(gene.value.length.toLong()) || (p < (1.0 - pOfLen) && anyCharToMutate && gene.value.isNotBlank())){
            return mutateChars(charsMutationUpdate, gene)
        }
        val append = pLength.toInt() > gene.value.length || (pLength.toInt() == gene.value.length && p < 1.0 - pOfLen/2.0)
        if (append){
            gene.value += randomness.nextWordChar() //(0 until (pLength.toInt() - gene.value.length)).map {randomness.nextWordChar()}.joinToString("")
        }else{
            gene.value = gene.value.dropLast(1)
        }
        return true
    }

    /**
     * select a char of [gene] to mutate based on a number of char candidates using weight based solution
     * ie, less candidates, higher weight
     * @param charsMutationUpdate collects the candidates
     * @param gene that is String to mutate
     */
    private fun mutateChars(charsMutationUpdate : List<LongMutationUpdate>, gene : StringGene) : Boolean {
        val weightsMap = charsMutationUpdate.mapIndexed { index, intMutationUpdate ->
            index to intMutationUpdate
        }.filter{ !it.second.isReached(gene.value[it.first].toLong()) }.map { it.first to it.second.candidatesBoundary().toDouble() }.toMap()
        if (weightsMap.isEmpty()) {
            log.warn("none of chars to select for the mutation")
            return false
        }

        val chars = mwc.selectSubsetWithWeight(weightsMap, true, mwc.getNGeneToMutate(weightsMap.size, 1))
        chars.forEach {
            val mc = charsMutationUpdate[it].random(
                    current = gene.value[it].toLong(),
                    randomness = randomness,
                    apc = apc,
                    start = 6,
                    end = 3,
                    probOfMiddle = probOfMiddle(charsMutationUpdate[it]),
                    minimalTimeForUpdate = 3
            ).toChar()
            gene.value = modifyIndex(gene.value, index = it, char = mc)
        }
        return true
    }

    fun mutateStringGene(
            gene: StringGene, targets: Set<Int>,
            allGenes : List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, additionalGeneMutationInfo: AdditionalGeneMutationInfo){
        var employBinding = true
        if (additionalGeneMutationInfo.impact == null){
            val ds = gene.standardSpecializationMutation(
                    randomness = randomness,
                    allGenes = allGenes,
                    selectionStrategy = selectionStrategy,
                    additionalGeneMutationInfo = additionalGeneMutationInfo,
                    apc = apc,
                    mwc = mwc,
                    enableAdaptiveGeneMutation = true
            )
            if (ds){
                return
            }
        }else{
            if (additionalGeneMutationInfo.impact !is StringGeneImpact)
                    throw IllegalArgumentException("mismatched GeneImpact for StringGene, ${additionalGeneMutationInfo.impact}")

            val impact = additionalGeneMutationInfo.impact

            val preferSpec = gene.specializationGenes.isNotEmpty()
            val specializationGene = gene.getSpecializationGene()

            val employSpec = doEmploy(impact = impact.employSpecialization, targets = targets)
            employBinding = doEmploy(impact = impact.employBinding, targets = targets)
            if (preferSpec && employSpec){
                if (specializationGene == null){
                    gene.selectedSpecialization = randomness.nextInt(0, gene.specializationGenes.size - 1)
                }else {
                    val selected = selectSpec(gene, impact, targets)
                    val currentImpact = impact.getSpecializationImpacts().getOrNull(gene.selectedSpecialization)
                    if (selected == gene.selectedSpecialization || currentImpact?.recentImprovement() == true){
                        specializationGene.standardMutation(
                                randomness, apc, mwc, allGenes,selectionStrategy, true, additionalGeneMutationInfo.copyFoInnerGene(currentImpact as? GeneImpact)
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
        }

        if (gene.redoTaint(apc, randomness, allGenes)) return

        if(additionalGeneMutationInfo.hasHistory())
            historyBasedValueMutation(additionalGeneMutationInfo, gene, allGenes)
        else
            gene.standardValueMutation(randomness, allGenes, apc)
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
        val impacts = mutableListOf<Impact>()
        if (impact.getSpecializationImpacts().size != gene.specializationGenes.size){
            log.warn("mismatched specialization impacts, {} impact but {} spec", impact.getSpecializationImpacts().size, gene.specializationGenes.size)
            if(impact.getSpecializationImpacts().isEmpty()){
                return randomness.nextInt(0, gene.specializationGenes.size - 1)
            }
        }

        if (gene.specializationGenes.size <= impact.getSpecializationImpacts().size){
            impacts.addAll(
                    impact.getSpecializationImpacts().subList(0, gene.specializationGenes.size)
            )
        }else{
            (impact.getSpecializationImpacts().size-1 until gene.specializationGenes.size).forEach {
                val ms = gene.specializationGenes[it]
                impacts.add(ImpactUtils.createGeneImpact(ms,ms.name))
            }
        }

        val weights = ags.impactBasedOnWeights(impacts = impacts, targets = targets)

        val selected = mwc.selectSubsetWithWeight(
                weights = weights.mapIndexed { index, d -> index to d }.toMap(),
                forceNotEmpty = true,
                numToMutate = 1.0
        )
        return randomness.choose(selected)
    }



    /**
     * during later phase of search, modify char/int with relative close genes
     */
    private fun randomFromCurrentAdaptively(
            current: Int, minValue: Int, maxValue: Int, hardMinValue: Int, hardMaxValue: Int, start: Int, end: Int): Int {
        val prefer = true
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

    /**
     * extract mutated info only for standard mutation
     */
    private fun mutatedGenePairForIndividualWithActions(
            originalActions: List<Action>, mutatedActions : List<Action>, mutatedGenes: List<Gene>, genesAtActionIndex: List<Int>
    ) : MutableList<Pair<Gene, Gene>>{
        Lazy.assert {
            mutatedActions.isEmpty()  || mutatedActions.size > genesAtActionIndex.max()!!
            mutatedActions.isEmpty()  || mutatedGenes.size == genesAtActionIndex.size
            originalActions.size == mutatedActions.size
        }

        val pairs = mutatedGenes.mapIndexed { index, gene ->
            ImpactUtils.findMutatedGene(originalActions[genesAtActionIndex[index]], gene) to ImpactUtils.findMutatedGene(mutatedActions[genesAtActionIndex[index]], gene)
        }

        if (pairs.isEmpty())
            log.warn("none of genes is mutated!")

        if (pairs.none { it.first == null || it.second == null }){
            return  pairs as MutableList<Pair<Gene, Gene>>
        }

        val ipairs = mutableListOf<Pair<Gene, Gene>>()
        originalActions.forEachIndexed { index, action ->
            val maction = mutatedActions.elementAt(index)
            action.seeGenes().filter { it.isMutable()}.forEach {g->
                val m = ImpactUtils.findMutatedGene(maction, g)
                if (m != null)
                    ipairs.add(g to m)
            }
        }
        if (pairs.isEmpty())
            log.warn("none of genes is mutated!")

        return ipairs

    }

    //FIXME MAN
    private fun createCharMutationUpdate() = LongMutationUpdate(getDefaultCharMin(), getDefaultCharMax())

    private fun getCharPool() = CharPool.WORD

    private fun getDefaultCharMin() = when(getCharPool()){
        CharPool.ALL -> Char.MIN_VALUE.toInt()
        CharPool.WORD -> randomness.wordCharPool().first()
    }

    private fun getDefaultCharMax() = when(getCharPool()){
        CharPool.ALL -> Char.MAX_VALUE.toInt()
        CharPool.WORD -> randomness.wordCharPool().last()
    }

    fun saveMutatedGene(mutatedGenes: MutatedGeneSpecification?, individual: Individual, index : Int, evaluatedMutation : EvaluatedMutation, targets: Set<Int>){
        ArchiveMutationUtils.saveMutatedGene(config, mutatedGenes, individual, index, evaluatedMutation, targets)
    }

}

enum class CharPool {
    ALL,
    WORD
}