package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactInfoCollection.value.date.DateTimeGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
open class DateTimeGene(
        name: String,
        val date: DateGene = DateGene("date"),
        val time: TimeGene = TimeGene("time"),
        val dateTimeGeneFormat: DateTimeGeneFormat = DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT
) : Gene(name) {

    enum class DateTimeGeneFormat {
        // YYYY-MM-DDTHH:SS:MM
        ISO_LOCAL_DATE_TIME_FORMAT,
        // YYYY-MM-DD HH:SS:MM
        DEFAULT_DATE_TIME
    }

    companion object{
        val log : Logger = LoggerFactory.getLogger(DateTimeGene::class.java)
    }

    init {
        date.parent = this
        time.parent = this
    }



    override fun copy(): Gene = DateTimeGene(
            name,
            date.copy() as DateGene,
            time.copy() as TimeGene,
            dateTimeGeneFormat = this.dateTimeGeneFormat
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        /**
         * If forceNewValue==true both date and time
         * get a new value, but it only might need
         * one to be different to get a new value.
         *
         * Shouldn't this method decide randomly if
         * date, time or both get a new value?
         */
        date.randomize(randomness, forceNewValue, allGenes)
        time.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return listOf(date, time)
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DateTimeGeneImpact){
            val maps = mapOf(
                    date to additionalGeneMutationInfo.impact.dateGeneImpact,
                    time to additionalGeneMutationInfo.impact.timeGeneImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it)) }
        }
        throw IllegalArgumentException("impact is null or not DateTimeGeneImpact")
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, EvaluatedMutation>, archiveMutator: ArchiveGeneMutator) {
        if (original !is DateTimeGene){
            log.warn("original ({}) should be DateTimeGene", original::class.java.simpleName)
            return
        }
        if (mutated !is DateTimeGene){
            log.warn("mutated ({}) should be DateTimeGene", mutated::class.java.simpleName)
            return
        }

        if (!mutated.date.containsSameValueAs(original.date)){
            date.archiveMutationUpdate(original.date, mutated.date, targetsEvaluated, archiveMutator)
        }
        if (!mutated.time.containsSameValueAs(original.time))
            time.archiveMutationUpdate(original.time, mutated.time, targetsEvaluated, archiveMutator)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        val formattedDate = GeneUtils.let {
            "${it.padded(date.year.value, 4)}-${it.padded(date.month.value, 2)}-${it.padded(date.day.value, 2)}"
        }
        val formattedTime = GeneUtils.let {
            "${it.padded(time.hour.value, 2)}:${it.padded(time.minute.value, 2)}:${it.padded(time.second.value, 2)}"
        }
        return when (dateTimeGeneFormat) {
            DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT -> {
                "${formattedDate}T${formattedTime}"
            }

            DateTimeGeneFormat.DEFAULT_DATE_TIME -> {
                "${formattedDate} ${formattedTime}"
            }
        }

    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DateTimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.date.copyValueFrom(other.date)
        this.time.copyValueFrom(other.time)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DateTimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.date.containsSameValueAs(other.date)
                && this.time.containsSameValueAs(other.time)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(date.flatView(excludePredicate)).plus(time.flatView(excludePredicate))
    }

    /*
     override fun mutationWeight(): Int
     weight for date time gene might be 1 as default since it is simple to solve
    */
}