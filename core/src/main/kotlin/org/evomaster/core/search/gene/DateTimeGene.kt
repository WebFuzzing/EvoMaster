package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.value.date.DateTimeGeneImpact
import org.evomaster.core.search.impact.value.date.TimeGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator

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

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        val gene = randomness.choose(listOf(date, time))
        gene.standardMutation(randomness, apc, allGenes)
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: ImpactMutationSelection, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>) {
        var genes : List<Pair<Gene, GeneImpact>>? = null
        val selects = if(impact != null && impact is DateTimeGeneImpact && archiveMutator.enableArchiveSelection()){
            genes = listOf(
                    Pair(date, impact.dateGeneImpact),
                    Pair(time , impact.timeGeneImpact)
            )
            archiveMutator.selectGenesByArchive(genes, 1.0/3)
        }else listOf(date, time)

        val selected = randomness.choose(selects)
        val selectedImpact = genes?.first { it.first == selected }?.second
        selected.archiveMutation(randomness, allGenes, apc, selection, selectedImpact, geneReference,archiveMutator, evi)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
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
}