package org.evomaster.core.search.service.mutator.genemutation

import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
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
    lateinit var ags : ArchiveImpactSelector


    companion object{
        private val log: Logger = LoggerFactory.getLogger(ArchiveGeneMutator::class.java)
    }

    private fun manageHistory(additionalGeneMutationInfo: AdditionalGeneMutationInfo, targets: Set<Int>) : List<Pair<Gene, EvaluatedInfo>> {
        return when (config.archiveGeneMutation.withTargets) {
            1 -> additionalGeneMutationInfo.history.filter { it.second.targets.any { t-> targets.contains(t) } && it.second.result?.isImpactful()?:true }
            2 -> additionalGeneMutationInfo.history.filter { it.second.specificTargets.any { t-> targets.contains(t) } && it.second.result?.isImpactful()?:true }
            else -> additionalGeneMutationInfo.history
        }
    }

    /**
     * apply mutation based on tracked history
     * @param additionalGeneMutationInfo history of gene
     * @param gene to mutate
     * @param allGenes are other genes in the same individual
     */
    fun historyBasedValueMutation(additionalGeneMutationInfo: AdditionalGeneMutationInfo, gene: Gene, allGenes: List<Gene>) {
        val history = manageHistory(additionalGeneMutationInfo, additionalGeneMutationInfo.targets)
        when (gene) {
            is StringGene -> {
                val applied = deriveMutatorForStringValue(history, gene, allGenes)
                // repair invalid char for string gene
                gene.repair()
                if (!applied) gene.standardValueMutation(randomness, allGenes, apc)
            }
            is IntegerGene -> gene.value = sampleValue(
                    history = history.map {
                        ((it.first as? IntegerGene)
                                ?: throw DifferentGeneInHistory(gene, it.first)
                                ).value.toLong() to (it.second.result?.value?:-2)
                    },
                    value = gene.value.toLong(),
                    valueUpdate = LongMutationUpdate(config.archiveGeneMutation.withDirection, min = gene.getMinimum(), max = gene.getMaximum()),
                    start = GeneUtils.intpow2.size, end = 10
            ).toInt()
            is LongGene -> gene.value =  sampleValue(
                    history = history.map {
                        ((it.first as? LongGene)
                                ?: throw DifferentGeneInHistory(gene, it.first)).value to (it.second.result?.value?:-2)
                    },
                    value = gene.value,
                    valueUpdate = LongMutationUpdate(config.archiveGeneMutation.withDirection, min = gene.getMinimum(), max = gene.getMaximum()),
                    start = GeneUtils.intpow2.size, end = 10
            )
            is DoubleGene -> gene.value =  sampleValue(
                    history = history.map {
                        ((it.first as? DoubleGene)?: throw DifferentGeneInHistory(gene, it.first)).value to (it.second.result?.value?:-2)
                    },
                    value = gene.value,
                    valueUpdate = DoubleMutationUpdate(
                        config.archiveGeneMutation.withDirection,
                        min = gene.getMinimum(), max = gene.getMaximum(), precision = gene.precision, scale = gene.scale),
                    start = GeneUtils.intpow2.size, end = 10
            )
            is FloatGene -> gene.value = sampleValue(
                    history = history.map {
                        ((it.first as? FloatGene)?: throw DifferentGeneInHistory(gene, it.first)).value.toDouble() to (it.second.result?.value?:-2)
                    },
                    value = gene.value.toDouble(),
                    valueUpdate = DoubleMutationUpdate(
                        config.archiveGeneMutation.withDirection,
                        min = gene.getMinimum().toDouble(), max = gene.getMaximum().toDouble(), precision = gene.precision, scale = gene.scale),
                    start = GeneUtils.intpow2.size, end = 10
            ).toFloat()

            is BigIntegerGene -> {
                val bihistory = history.map {
                    ((it.first as? BigIntegerGene)?: throw DifferentGeneInHistory(gene, it.first)).value.toLong() to (it.second.result?.value?:EvaluatedMutation.UNSURE.value)
                }

                val lvalue = sampleValue(
                    history = bihistory,
                    value =  gene.toLong(),
                    valueUpdate = LongMutationUpdate(config.archiveGeneMutation.withDirection, min = gene.min?.toLong()?: Long.MIN_VALUE, max = gene.max?.toLong()?: Long.MAX_VALUE),
                    start = GeneUtils.intpow2.size, end = 10
                )
                gene.setValueWithLong(lvalue)


            }
            is BigDecimalGene ->{
                if (gene.floatingPointMode){
                    val bdhistory = history.map {
                        ((it.first as? BigDecimalGene)?: throw DifferentGeneInHistory(gene, it.first)).value.toDouble() to (it.second.result?.value?:EvaluatedMutation.UNSURE.value)
                    }
                    val fvalue = sampleValue(
                        history = bdhistory,
                        value =  gene.toDouble(),
                        valueUpdate = DoubleMutationUpdate(config.archiveGeneMutation.withDirection,
                        min = gene.getMinimum().toDouble(), max = gene.getMaximum().toDouble(), precision = gene.precision, scale = gene.scale),
                        start = GeneUtils.intpow2.size, end = 10)
                    gene.setValueWithDouble(fvalue)
                }else{

                    val bdhistory = history.map {
                        ((it.first as? BigDecimalGene)?: throw DifferentGeneInHistory(gene, it.first)).value.toLong() to (it.second.result?.value?:EvaluatedMutation.UNSURE.value)
                    }
                    val lvalue = sampleValue(
                        history = bdhistory,
                        value =  gene.toLong(),
                        valueUpdate = LongMutationUpdate(config.archiveGeneMutation.withDirection, min = gene.min?.toLong()?: Long.MIN_VALUE, max = gene.max?.toLong()?: Long.MAX_VALUE),
                        start = GeneUtils.intpow2.size, end = 10
                    )
                    gene.setValueWithLong(lvalue)
                }
            }

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

    private fun<T: Number> sampleValue(history : List<Pair<T, Int>>, value: T, valueUpdate: MutationBoundaryUpdate<T>, start: Int, end: Int) : T {
        (0 until history.size).forEach {i->
            valueUpdate.updateOrRestBoundary(
                    index = i,
                    current = history[i].first,
                    evaluatedResult = history[i].second
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

        val lenMutationUpdate = LongMutationUpdate(config.archiveGeneMutation.withDirection, gene.minLength.toLong(), gene.maxLength.toLong())
        val charsMutationUpdate = (0 until gene.value.length).map { createCharMutationUpdate() }
        (0 until history.size).forEach { i ->
            val c = ( history[i].first as? StringGene)?.value ?: throw IllegalStateException("invalid extracted history element")
            val better = history[i].second.result?.value?:-2

            lenMutationUpdate.updateOrRestBoundary(
                    index = i,
                    current = c.length.toLong(),
                    evaluatedResult = better
            )
            charsMutationUpdate.forEachIndexed { index, intMutationUpdate ->
                if (c.elementAtOrNull(index) != null){
                    intMutationUpdate.updateOrRestBoundary(
                            index = i,
                            current = c.elementAt(index).toLong(),
                            evaluatedResult = better
                    )
                }
            }
        }

        val p = randomness.nextDouble()
        val pOfLen = apc.getExploratoryValue(0.6, 0.2)

        val anyCharToMutate = charsMutationUpdate.filterIndexed {
            index, longMutationUpdate -> !longMutationUpdate.isReached(gene.value[index].toLong()) }.isNotEmpty() && charsMutationUpdate.isNotEmpty()
        if (!anyCharToMutate && lenMutationUpdate.isReached(gene.value.length.toLong())) return false

        if (p < 0.02 && others.isNotEmpty()){
            gene.value = randomness.choose(others)
            return true
        }

        if (lenMutationUpdate.isReached(gene.value.length.toLong()) || !lenMutationUpdate.isUpdatable() || (p < (1.0 - pOfLen) && anyCharToMutate && gene.value.isNotBlank())){
            return mutateChars(charsMutationUpdate, gene)
        }

        val pLength = lenMutationUpdate.random(
            apc = apc,
            randomness = randomness,
            current = gene.value.length.toLong(),
            probOfMiddle = probOfMiddle(lenMutationUpdate),
            start = 6,
            end = 3,
            minimalTimeForUpdate = 2
        )

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

    /**
     * archive-based gene mutation for string gene
     * @param gene to mutate
     * @param targets for this mutation
     * @param allGenes are other genes in the same individual
     * @param selectionStrategy indicates the startegy to select a specialization of the gene
     * @param additionalGeneMutationInfo contains addtional info for applying archive-based gene mutation, e.g., impact, history of the gene
     */
    fun mutateStringGene(
        gene: StringGene, targets: Set<Int>,
        allGenes : List<Gene>, selectionStrategy: SubsetGeneMutationSelectionStrategy, additionalGeneMutationInfo: AdditionalGeneMutationInfo, changeSpecSetting: Double){
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
                    var selected = selectSpec(gene, impact, targets)
                    val currentImpact = impact.getSpecializationImpacts().getOrNull(gene.selectedSpecialization)

                    val selectCurrent = gene.selectedSpecialization == selected || (currentImpact?.recentImprovement() == true && randomness.nextBoolean(1.0-changeSpecSetting))

                    if (selectCurrent && specializationGene.isMutable()){
                        specializationGene.standardMutation(
                            randomness, apc, mwc,selectionStrategy, true, additionalGeneMutationInfo.copyFoInnerGene(currentImpact as? GeneImpact)
                        )
                    }else if (gene.selectedSpecialization == selected){
                        selected = (selected + 1) % gene.specializationGenes.size
                        gene.selectedSpecialization = selected
                    } else{
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
        if (gene.redoTaint(apc, randomness)) return

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
            mutatedActions.isEmpty()  || mutatedActions.size > genesAtActionIndex.maxOrNull()!!
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
            action.seeTopGenes().filter { it.isMutable()}.forEach { g->
                val m = ImpactUtils.findMutatedGene(maction, g)
                if (m != null)
                    ipairs.add(g to m)
            }
        }
        if (pairs.isEmpty())
            log.warn("none of genes is mutated!")

        return ipairs

    }

    private fun createCharMutationUpdate() = LongMutationUpdate(config.archiveGeneMutation.withDirection,getDefaultCharMin(), getDefaultCharMax())

    private fun getCharPool() = CharPool.WORD

    private fun getDefaultCharMin() = when(getCharPool()){
        CharPool.ALL -> Char.MIN_VALUE.toInt()
        CharPool.WORD -> randomness.wordCharPool().first()
    }

    private fun getDefaultCharMax() = when(getCharPool()){
        CharPool.ALL -> Char.MAX_VALUE.toInt()
        CharPool.WORD -> randomness.wordCharPool().last()
    }

    /**
     * save detailed mutated gene over search which is useful for debugging
     * @param mutatedGenes contains what gene are mutated in this evaluation
     * @param individual is the individual to mutate
     * @param index indicates timepoint over search
     * @param evaluatedMutation is evaluated result of this mutation
     * @param targets are targets for this mutation
     */
    fun saveMutatedGene(mutatedGenes: MutatedGeneSpecification?, individual: Individual, index : Int, evaluatedMutation : EvaluatedMutation, targets: Set<Int>){
        ArchiveMutationUtils.saveMutatedGene(config, mutatedGenes, individual, index, evaluatedMutation, targets)
    }

}

/**
 * which chars are used for sampling string gene
 * this might need to be further improved
 */
enum class CharPool {
    ALL,
    WORD
}