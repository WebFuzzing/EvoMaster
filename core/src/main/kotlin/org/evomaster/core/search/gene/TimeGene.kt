package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.TimeGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
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
    val hour: IntegerGene = IntegerGene("hour", 0, MIN_HOUR, MAX_HOUR),
    val minute: IntegerGene = IntegerGene("minute", 0, MIN_MINUTE, MAX_MINUTE),
    val second: IntegerGene = IntegerGene("second", 0, MIN_SECOND, MAX_SECOND),
    val timeGeneFormat: TimeGeneFormat = TimeGeneFormat.TIME_WITH_MILLISECONDS
) : Gene(name, mutableListOf<Gene>(hour, minute, second)) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(TimeGene::class.java)
        const val MAX_HOUR = 25
        const val MIN_HOUR = -1
        const val MAX_MINUTE = 60
        const val MIN_MINUTE = -1
        const val MAX_SECOND = 60
        const val MIN_SECOND = -1
    }

    enum class TimeGeneFormat {
        // format HH:MM:SS
        ISO_LOCAL_DATE_FORMAT,

        // HH:MM:SS.000Z
        TIME_WITH_MILLISECONDS
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(hour, minute, second)

    /*
        Note: would need to handle timezone and second fractions,
        but not sure how important for testing purposes
     */

    override fun copyContent(): Gene = TimeGene(
            name,
            hour.copyContent() as IntegerGene,
            minute.copyContent() as IntegerGene,
            second.copyContent() as IntegerGene,
            timeGeneFormat = this.timeGeneFormat
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        hour.randomize(randomness, forceNewValue, allGenes)
        minute.randomize(randomness, forceNewValue, allGenes)
        second.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return listOf(hour, minute, second)
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is TimeGeneImpact){
            val maps = mapOf<Gene,  GeneImpact>(
                    hour to additionalGeneMutationInfo.impact.hourGeneImpact,
                    minute to additionalGeneMutationInfo.impact.minuteGeneImpact,
                    second to additionalGeneMutationInfo.impact.secondGeneImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi)
                    .map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }
        throw IllegalArgumentException("impact is null or not TimeGeneImpact")
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
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

    override fun innerGene(): List<Gene> = listOf(hour, minute, second)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when{
            gene is TimeGene->{
                hour.bindValueBasedOn(gene.hour) &&
                        second.bindValueBasedOn(gene.minute) &&
                        minute.bindValueBasedOn(gene.second)
            }
            gene is DateTimeGene -> bindValueBasedOn(gene.time)
            gene is StringGene && gene.getSpecializationGene() != null-> bindValueBasedOn(gene.getSpecializationGene()!!)
            else ->{
                LoggingUtil.uniqueWarn(log, "cannot bind TimeGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun repair() {
        if (hour.value < 0) {
            hour.value = 0
        } else if (hour.value > 23) {
            hour.value = 23
        }

        if (minute.value < 0) {
            minute.value = 0
        } else if (minute.value > 59) {
            minute.value = 59
        }

        if (second.value < 0) {
            second.value = 0
        } else if (second.value > 59) {
            second.value = 59
        }
    }
}