package org.evomaster.core.search.gene.datetime

import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.DateGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
class DateGene(
    name: String,
    //note: ranges deliberately include wrong values.
    val year: IntegerGene = IntegerGene("year", 2016, MIN_YEAR, MAX_YEAR),
    val month: IntegerGene = IntegerGene("month", 3, MIN_MONTH, MAX_MONTH),
    val day: IntegerGene = IntegerGene("day", 12, MIN_DAY, MAX_DAY),
    val onlyValidDates: Boolean = false, //TODO refactor once dealing with Robustness Testing
    val dateGeneFormat: DateGeneFormat = DateGeneFormat.ISO_LOCAL_DATE_FORMAT
) : ComparableGene, CompositeFixedGene(name, listOf(year, month, day)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(DateGene::class.java)
        const val MAX_YEAR = 2100
        const val MIN_YEAR = 1900
        const val MAX_MONTH = 13
        const val MIN_MONTH = 0
        const val MAX_DAY = 32
        const val MIN_DAY = 0

        val DATE_GENE_COMPARATOR = compareBy<DateGene> { it.year }
            .thenBy { it.month }
            .thenBy { it.day }

    }

    enum class DateGeneFormat {
        ISO_LOCAL_DATE_FORMAT
    }

    override fun isLocallyValid() : Boolean{
        return getViewOfChildren().all { it.isLocallyValid() }
    }

    override fun copyContent(): Gene = DateGene(
        name,
        year.copy() as IntegerGene,
        month.copy() as IntegerGene,
        day.copy() as IntegerGene,
        dateGeneFormat = this.dateGeneFormat,
        onlyValidDates = this.onlyValidDates
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        do {
            year.randomize(randomness, tryToForceNewValue)
            month.randomize(randomness, tryToForceNewValue)
            day.randomize(randomness, tryToForceNewValue)
        } while (onlyValidDates && !isValidDate())
    }



    override fun mutationCheck(): Boolean {
        return !onlyValidDates || isValidDate()
    }

    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DateGeneImpact) {
            val maps = mapOf<Gene, GeneImpact>(
                year to additionalGeneMutationInfo.impact.yearGeneImpact,
                month to additionalGeneMutationInfo.impact.monthGeneImpact,
                day to additionalGeneMutationInfo.impact.dayGeneImpact
            )
            return mwc.selectSubGene(
                internalGenes,
                adaptiveWeight = true,
                targets = additionalGeneMutationInfo.targets,
                impacts = internalGenes.map { i -> maps.getValue(i) },
                individual = null,
                evi = additionalGeneMutationInfo.evi
            ).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }

        throw IllegalArgumentException("impact is null or not DateGeneImpact")
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return if (mode == GeneUtils.EscapeMode.EJSON) {
            val millis = LocalDate.of(year.value, month.value, day.value).atStartOfDay().atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            "{\"\$date\":{\"\$numberLong\":\"$millis\"}}"
        } else {
            "\"${getValueAsRawString()}\""
        }
    }

    override fun getValueAsRawString(): String {
        return when (dateGeneFormat) {
            DateGeneFormat.ISO_LOCAL_DATE_FORMAT -> GeneUtils.let {
                "${GeneUtils.padded(year.value, 4)}-${GeneUtils.padded(month.value, 2)}-${
                    GeneUtils.padded(
                        day.value,
                        2
                    )
                }"
            }
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is DateGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.onlyValidDates && !other.isValidDate()) {
            throw IllegalArgumentException(
                "Cannot copy invalid date %s to gene accepting only valid values".format(
                    other.getValueAsRawString()
                )
            )
        }

        return updateValueOnlyIfValid(
            {this.year.copyValueFrom(other.year) && this.month.copyValueFrom(other.month) && this.day.copyValueFrom(other.day)}, true
        )
    }

    /**
     * Checks if the current date is valid or not (e.g. max days in months, leap years)
     */
    private fun isValidDate(): Boolean {
        return try {
            LocalDate.parse(getValueAsRawString(), DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (ex: DateTimeParseException) {
            false
        }
    }


    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DateGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.year.containsSameValueAs(other.year)
                && this.month.containsSameValueAs(other.month)
                && this.day.containsSameValueAs(other.day)
    }



    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is DateGene -> {
                day.bindValueBasedOn(gene.day) &&
                        month.bindValueBasedOn(gene.month) &&
                        year.bindValueBasedOn(gene.year)
            }
            gene is DateTimeGene -> bindValueBasedOn(gene.date)
            gene is StringGene && gene.getSpecializationGene() != null -> bindValueBasedOn(gene.getSpecializationGene()!!)
            gene is SeededGene<*> -> this.bindValueBasedOn(gene.getPhenotype() as Gene)
            // Man: convert to string based on the format?
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind DateGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun repair() {
        if (month.value < 1) {
            month.value = 1
        } else if (month.value > 12) {
            month.value = 12
        }

        if (day.value < 1) {
            day.value = 1
        }

        //February
        if (month.value == 2 && day.value > 28) {
            //for simplicity, let's not consider cases in which 29...
            day.value = 28
        } else if (day.value > 30 && (month.value.let { it == 11 || it == 4 || it == 6 || it == 9 })) {
            day.value = 30
        } else if (day.value > 31) {
            day.value = 31
        }
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is DateGene) {
            throw ClassCastException("Expected DataGene instance but ${other::javaClass} was found")
        }
        return DATE_GENE_COMPARATOR.compare(this, other)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}