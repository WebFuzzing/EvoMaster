package org.evomaster.core.search.gene.datetime

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.DateTimeGeneImpact
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
open class DateTimeGene(
    name: String,
    val onlyValid : Boolean = false, //TODO refactor once dealing with Robustness Testing
    val format: FormatForDatesAndTimes = FormatForDatesAndTimes.ISO_LOCAL,
    val date: DateGene = DateGene("date", format = format, onlyValidDates = onlyValid),
    val time: TimeGene = TimeGene("time", format = format, onlyValidTimes = onlyValid),
) : ComparableGene, CompositeFixedGene(name, listOf(date, time)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(DateTimeGene::class.java)

        val DATE_TIME_GENE_COMPARATOR = compareBy<DateTimeGene> { it.date }
            .thenBy { it.time }
    }

    init {
        if(format != date.format){
            throw IllegalArgumentException("Mismatched format for date: $format != ${date.format}")
        }
        if(format != time.format){
            throw IllegalArgumentException("Mismatched format for time: $format != ${time.format}")
        }
        if(onlyValid && (!date.onlyValidDates || !time.onlyValidTimes)) {
            throw IllegalArgumentException("Marked as onlyValid, but date=${date.onlyValidDates} and time=${time.onlyValidTimes}")
        }
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = DateTimeGene(
        name,
        onlyValid,
        format,
        date.copy() as DateGene,
        time.copy() as TimeGene,
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        /**
         * If forceNewValue==true both date and time
         * get a new value, but it only might need
         * one to be different to get a new value.
         *
         * Shouldn't this method decide randomly if
         * date, time or both get a new value?
         */
        date.randomize(randomness, tryToForceNewValue)
        time.randomize(randomness, tryToForceNewValue)
    }



    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DateTimeGeneImpact) {
            val maps = mapOf<Gene, GeneImpact>(
                date to additionalGeneMutationInfo.impact.dateGeneImpact ,
                time to additionalGeneMutationInfo.impact.timeGeneImpact
            )

            return mwc.selectSubGene(
                internalGenes,
                adaptiveWeight = true,
                targets = additionalGeneMutationInfo.targets,
                impacts = internalGenes.map { i  -> maps.getValue(i) },
                individual = null,
                evi = additionalGeneMutationInfo.evi
            ).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }
        throw IllegalArgumentException("impact is null or not DateTimeGeneImpact")
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

        val formattedDate = date.getValueAsRawString()
        val formattedTime = time.getValueAsRawString()

        return when (format) {
            FormatForDatesAndTimes.ISO_LOCAL, FormatForDatesAndTimes.RFC3339-> {
                "${formattedDate}T${formattedTime}"
            }

            FormatForDatesAndTimes.DATETIME-> {
                "$formattedDate $formattedTime"
            }
        }

    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is DateTimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.date.copyValueFrom(other.date) && this.time.copyValueFrom(other.time)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DateTimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.date.containsSameValueAs(other.date)
                && this.time.containsSameValueAs(other.time)
    }





    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is DateTimeGene -> {
                date.setValueBasedOn(gene.date) &&
                        time.setValueBasedOn(gene.time)
            }
            gene is DateGene -> date.setValueBasedOn(gene)
            gene is TimeGene -> time.setValueBasedOn(gene)
            gene is StringGene && gene.getSpecializationGene() != null -> {
                setValueBasedOn(gene.getSpecializationGene()!!)
            }
            gene is SeededGene<*> -> this.setValueBasedOn(gene.getPhenotype()as Gene)
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind DateTimeGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is DateTimeGene) {
            throw ClassCastException("Instance of DateTimeGene was expected but ${other::javaClass} was found")
        }
        return DATE_TIME_GENE_COMPARATOR.compare(this, other)
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