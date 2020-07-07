package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.DateGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
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
        val year: IntegerGene = IntegerGene("year", 2016, 1900, 2100),
        val month: IntegerGene = IntegerGene("month", 3, 0, 13),
        val day: IntegerGene = IntegerGene("day", 12, 0, 32),
        val onlyValidDates: Boolean = false,
        val dateGeneFormat: DateGeneFormat = DateGeneFormat.ISO_LOCAL_DATE_FORMAT
) : Gene(name) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(DateGene::class.java)
    }

    enum class DateGeneFormat {
        ISO_LOCAL_DATE_FORMAT
    }

    init {
        year.parent = this
        month.parent = this
        day.parent = this
    }


    override fun copy(): Gene = DateGene(name,
            year.copy() as IntegerGene,
            month.copy() as IntegerGene,
            day.copy() as IntegerGene,
            dateGeneFormat = this.dateGeneFormat,
            onlyValidDates = this.onlyValidDates)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        do {
            year.randomize(randomness, forceNewValue, allGenes)
            month.randomize(randomness, forceNewValue, allGenes)
            day.randomize(randomness, forceNewValue, allGenes)
        } while (onlyValidDates && !isValidDate())
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return listOf(year, month, day)
    }

    override fun mutationCheck(): Boolean {
        return !onlyValidDates || isValidDate()
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DateGeneImpact){
            val maps = mapOf<Gene, GeneImpact>(
                    year to additionalGeneMutationInfo.impact.yearGeneImpact,
                    month to additionalGeneMutationInfo.impact.monthGeneImpact,
                    day to additionalGeneMutationInfo.impact.dayGeneImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it)) }
        }

        throw IllegalArgumentException("impact is null or not DateGeneImpact")
    }


    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, EvaluatedMutation>, archiveMutator: ArchiveGeneMutator) {
        if (original !is DateGene){
            log.warn("original ({}) should be DateGene", original::class.java.simpleName)
            return
        }
        if (mutated !is DateGene){
            log.warn("mutated ({}) should be DateGene", mutated::class.java.simpleName)
            return
        }

        if (!mutated.year.containsSameValueAs(original.year)){
            year.archiveMutationUpdate(original.year, mutated.year, targetsEvaluated, archiveMutator)
        }
        if (!mutated.month.containsSameValueAs(original.month))
            month.archiveMutationUpdate(original.month, mutated.month, targetsEvaluated, archiveMutator)
        if (!mutated.day.containsSameValueAs(original.day))
            day.archiveMutationUpdate(original.day, mutated.day, targetsEvaluated, archiveMutator)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return when (dateGeneFormat) {
            DateGeneFormat.ISO_LOCAL_DATE_FORMAT -> GeneUtils.let {
                "${it.padded(year.value, 4)}-${it.padded(month.value, 2)}-${it.padded(day.value, 2)}"
            }
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DateGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.onlyValidDates && !other.isValidDate()) {
            throw IllegalArgumentException("Cannot copy invalid date %s to gene accepting only valid values".format(other.getValueAsRawString()))
        }
        this.year.copyValueFrom(other.year)
        this.month.copyValueFrom(other.month)
        this.day.copyValueFrom(other.day)
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

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(year.flatView(excludePredicate))
                    .plus(month.flatView(excludePredicate))
                    .plus(day.flatView(excludePredicate))
    }

    override fun reachOptimal(targets: Set<Int>): Boolean {
        return year.reachOptimal(targets) && month.reachOptimal(targets) && day.reachOptimal(targets)
    }

    /*
     override fun mutationWeight(): Int
     weight for date gene might be 1 as default since it is simple to solve
    */
}