package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.TimeGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
class TimeGene(
        name: String,
        //note: ranges deliberately include wrong values.
        val hour: IntegerGene = IntegerGene("hour", 0, -1, 25),
        val minute: IntegerGene = IntegerGene("minute", 0, -1, 60),
        val second: IntegerGene = IntegerGene("second", 0, -1, 60),
        val timeGeneFormat: TimeGeneFormat = TimeGeneFormat.TIME_WITH_MILLISECONDS
) : Gene(name) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(TimeGene::class.java)
    }

    enum class TimeGeneFormat {
        // format HH:MM:SS
        ISO_LOCAL_DATE_FORMAT,

        // HH:MM:SS.000Z
        TIME_WITH_MILLISECONDS
    }

    init {
        hour.parent = this
        minute.parent = this
        second.parent = this
    }


    /*
        Note: would need to handle timezone and second fractions,
        but not sure how important for testing purposes
     */

    override fun copy(): Gene = TimeGene(
            name,
            hour.copy() as IntegerGene,
            minute.copy() as IntegerGene,
            second.copy() as IntegerGene,
            timeGeneFormat = this.timeGeneFormat
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        hour.randomize(randomness, forceNewValue, allGenes)
        minute.randomize(randomness, forceNewValue, allGenes)
        second.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return listOf(hour, minute, second)
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is TimeGeneImpact){
            val maps = mapOf<Gene,  GeneImpact>(
                    hour to additionalGeneMutationInfo.impact.hourGeneImpact,
                    minute to additionalGeneMutationInfo.impact.minuteGeneImpact,
                    second to additionalGeneMutationInfo.impact.secondGeneImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it)) }
        }
        throw IllegalArgumentException("impact is null or not TimeGeneImpact")
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, EvaluatedMutation>, archiveMutator: ArchiveGeneMutator) {
        if (original !is TimeGene){
            log.warn("original ({}) should be TimeGene", original::class.java.simpleName)
            return
        }
        if (mutated !is TimeGene){
            log.warn("mutated ({}) should be TimeGene", mutated::class.java.simpleName)
            return
        }

        if (!mutated.hour.containsSameValueAs(original.hour)){
            hour.archiveMutationUpdate(original.hour, mutated.hour, targetsEvaluated, archiveMutator)
        }
        if (!mutated.minute.containsSameValueAs(original.minute))
            minute.archiveMutationUpdate(original.minute, mutated.minute, targetsEvaluated, archiveMutator)
        if (!mutated.second.containsSameValueAs(original.second))
            second.archiveMutationUpdate(original.second, mutated.second, targetsEvaluated, archiveMutator)
    }

    override fun reachOptimal(targets: Set<Int>): Boolean {
        return hour.reachOptimal(targets) && minute.reachOptimal(targets) && second.reachOptimal(targets)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return when (timeGeneFormat) {
            TimeGeneFormat.ISO_LOCAL_DATE_FORMAT -> {
                GeneUtils.let {
                    "${it.padded(hour.value, 2)}:${it.padded(minute.value, 2)}:${it.padded(second.value, 2)}"
                }
            }
            TimeGeneFormat.TIME_WITH_MILLISECONDS -> {
                GeneUtils.let {
                    "${it.padded(hour.value, 2)}:${it.padded(minute.value, 2)}:${it.padded(second.value, 2)}.000Z"
                }
            }
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is TimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.hour.copyValueFrom(other.hour)
        this.minute.copyValueFrom(other.minute)
        this.second.copyValueFrom(other.second)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.hour.containsSameValueAs(other.hour)
                && this.minute.containsSameValueAs(other.minute)
                && this.second.containsSameValueAs(other.second)
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(hour.flatView(excludePredicate))
                .plus(minute.flatView(excludePredicate))
                .plus(second.flatView(excludePredicate))
    }

    private fun isValidHourRange(gene: IntegerGene): Boolean {
        return gene.min == 0 && gene.max == 23
    }

    private fun isValidMinuteRange(gene: IntegerGene): Boolean {
        return gene.min == 0 && gene.max == 59
    }

    private fun isValidSecondRange(gene: IntegerGene): Boolean {
        return gene.min == 0 && gene.max == 59
    }

    /**
     * Queries the time gent to know if it can
     * store only valid hours (i.e. range 00:00:00 to 23:59:59)
     */
    val onlyValidHours: Boolean
        get() = isValidHourRange(this.hour)
                && isValidMinuteRange(this.minute)
                && isValidSecondRange(this.second)


    /*
     override fun mutationWeight(): Int
     weight for time gene might be 1 as default since it is simple to solve
    */
}