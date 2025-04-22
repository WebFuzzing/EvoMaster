package org.evomaster.core.search.gene.sql.time

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlTimeIntervalGene(
    name: String,
    val days: IntegerGene = IntegerGene(name = "days", min = 0),
    val time: TimeGene = TimeGene(
                "hoursMinutesAndSeconds",
                format = FormatForDatesAndTimes.ISO_LOCAL
        )
) : CompositeFixedGene(name, mutableListOf(days, time)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlTimeIntervalGene::class.java)
    }

    override fun copyContent(): Gene = SqlTimeIntervalGene(
            name,
            days.copy() as IntegerGene,
            time.copy() as TimeGene
    )

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        /**
         * If forceNewValue==true both date and time
         * get a new value, but it only might need
         * one to be different to get a new value.
         *
         * Shouldn't this method decide randomly if
         * date, time or both get a new value?
         */
        days.randomize(randomness, tryToForceNewValue)
        time.randomize(randomness, tryToForceNewValue)
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
        return "${days.value} days ${time.getValueAsRawString()}"
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlTimeIntervalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {this.days.copyValueFrom(other.days)
                    && this.time.copyValueFrom(other.time)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTimeIntervalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.days.containsSameValueAs(other.days)
                && this.time.containsSameValueAs(other.time)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlTimeIntervalGene -> {
                days.setValueBasedOn(gene.days) &&
                        time.setValueBasedOn(gene.time)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind IntervalGene with ${gene::class.java.simpleName}")
                false
            }
        }
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