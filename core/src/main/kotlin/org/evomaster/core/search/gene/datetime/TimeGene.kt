package org.evomaster.core.search.gene.datetime

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.TimeGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
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
    val timeGeneFormat: TimeGeneFormat = TimeGeneFormat.TIME_WITH_MILLISECONDS,
    val onlyValidTimes: Boolean = false //TODO refactor once dealing with Robustness Testing
) : Comparable<TimeGene>, CompositeFixedGene(name, listOf(hour, minute, second)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(TimeGene::class.java)
        const val MAX_HOUR = 25
        const val MIN_HOUR = -1
        const val MAX_MINUTE = 60
        const val MIN_MINUTE = -1
        const val MAX_SECOND = 60
        const val MIN_SECOND = -1

        val TIME_GENE_COMPARATOR: Comparator<TimeGene> = Comparator
            .comparing(TimeGene::hour)
            .thenBy(TimeGene::minute)
            .thenBy(TimeGene::second)

    }

    enum class TimeGeneFormat {
        // format HH:MM:SS
        ISO_LOCAL_DATE_FORMAT,

        // HH:MM:SS.000Z
        TIME_WITH_MILLISECONDS
    }

    override fun isLocallyValid() : Boolean{
        return getViewOfChildren().all { it.isLocallyValid() }
    }

    /*
        Note: would need to handle timezone and second fractions,
        but not sure how important for testing purposes
     */

    override fun copyContent(): Gene = TimeGene(
        name,
        hour.copy() as IntegerGene,
        minute.copy() as IntegerGene,
        second.copy() as IntegerGene,
        timeGeneFormat = this.timeGeneFormat,
        onlyValidTimes = this.onlyValidTimes
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        do {
            hour.randomize(randomness, tryToForceNewValue)
            minute.randomize(randomness, tryToForceNewValue)
            second.randomize(randomness, tryToForceNewValue)
        } while (onlyValidTimes && !isValidTime())
    }

    fun isValidTime()= hour.value in 0..23
            && minute.value in 0..59
            && second.value in 0..59

    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is TimeGeneImpact) {
            val maps = mapOf<Gene, GeneImpact>(
                hour to additionalGeneMutationInfo.impact.hourGeneImpact,
                minute to additionalGeneMutationInfo.impact.minuteGeneImpact,
                second to additionalGeneMutationInfo.impact.secondGeneImpact
            )
            return mwc.selectSubGene(
                internalGenes,
                adaptiveWeight = true,
                targets = additionalGeneMutationInfo.targets,
                impacts = internalGenes.map { i -> maps.getValue(i) },
                individual = null,
                evi = additionalGeneMutationInfo.evi
            )
                .map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }
        throw IllegalArgumentException("impact is null or not TimeGeneImpact")
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return when (timeGeneFormat) {
            TimeGeneFormat.ISO_LOCAL_DATE_FORMAT -> {
                GeneUtils.let {
                    "${GeneUtils.padded(hour.value, 2)}:${
                        GeneUtils.padded(
                            minute.value,
                            2
                        )
                    }:${GeneUtils.padded(second.value, 2)}"
                }
            }
            TimeGeneFormat.TIME_WITH_MILLISECONDS -> {
                GeneUtils.let {
                    "${GeneUtils.padded(hour.value, 2)}:${
                        GeneUtils.padded(
                            minute.value,
                            2
                        )
                    }:${GeneUtils.padded(second.value, 2)}.000Z"
                }
            }
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is TimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {this.hour.copyValueFrom(other.hour)
                    && this.minute.copyValueFrom(other.minute)
                    && this.second.copyValueFrom(other.second)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.hour.containsSameValueAs(other.hour)
                && this.minute.containsSameValueAs(other.minute)
                && this.second.containsSameValueAs(other.second)
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




    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is TimeGene -> {
                hour.bindValueBasedOn(gene.hour) &&
                        second.bindValueBasedOn(gene.minute) &&
                        minute.bindValueBasedOn(gene.second)
            }
            gene is DateTimeGene -> bindValueBasedOn(gene.time)
            gene is StringGene && gene.getSpecializationGene() != null -> bindValueBasedOn(gene.getSpecializationGene()!!)
            gene is SeededGene<*> -> this.bindValueBasedOn(gene.getPhenotype()  as Gene)
            else -> {
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

    override fun compareTo(other: TimeGene): Int {
        return TIME_GENE_COMPARATOR.compare(this, other)
    }


    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    override fun mutationCheck(): Boolean {
        return !onlyValidTimes || isValidTime()
    }

}