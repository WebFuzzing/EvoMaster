package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.value.date.DateTimeGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
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
) : Gene(name, mutableListOf(date, time)) {

    enum class DateTimeGeneFormat {
        // YYYY-MM-DDTHH:SS:MM
        ISO_LOCAL_DATE_TIME_FORMAT,
        // YYYY-MM-DD HH:SS:MM
        DEFAULT_DATE_TIME
    }

    companion object{
        val log : Logger = LoggerFactory.getLogger(DateTimeGene::class.java)
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(date, time)

    override fun copyContent(): Gene = DateTimeGene(
            name,
            date.copyContent() as DateGene,
            time.copyContent() as TimeGene,
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

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return listOf(date, time)
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DateTimeGeneImpact){
            val maps = mapOf(
                    date to additionalGeneMutationInfo.impact.dateGeneImpact,
                    time to additionalGeneMutationInfo.impact.timeGeneImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }
        throw IllegalArgumentException("impact is null or not DateTimeGeneImpact")
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
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

    override fun innerGene(): List<Gene> = listOf(date, time)


    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when{
            gene is DateTimeGene -> {
                date.bindValueBasedOn(gene.date) &&
                        time.bindValueBasedOn(gene.time)
            }
            gene is DateGene -> date.bindValueBasedOn(gene)
            gene is TimeGene -> time.bindValueBasedOn(gene)
            gene is StringGene && gene.getSpecializationGene()!= null -> {
                bindValueBasedOn(gene.getSpecializationGene()!!)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind DateTimeGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }
}