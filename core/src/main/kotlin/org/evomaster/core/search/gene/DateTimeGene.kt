package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.value.date.DateTimeGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
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

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) {
        if (!enableAdaptiveGeneMutation){
            val gene = randomness.choose(listOf(date, time))
            gene.standardMutation(randomness, apc, allGenes)
            return
        }
        additionalGeneMutationInfo?:throw IllegalArgumentException("additionalGeneMutationInfo should not be null when enable adaptive gene mutation")
        archiveMutation(randomness, apc, allGenes, additionalGeneMutationInfo)

    }

    private fun archiveMutation(randomness: Randomness,  apc: AdaptiveParameterControl, allGenes: List<Gene>, additionalGeneMutationInfo: AdditionalGeneMutationInfo) {

        var genes : List<Pair<Gene, GeneImpact>>? = null
        val selects = if(additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DateTimeGeneImpact && additionalGeneMutationInfo.archiveMutator.applyArchiveSelection()){
            genes = listOf(
                    Pair(date, additionalGeneMutationInfo.impact.dateGeneImpact),
                    Pair(time , additionalGeneMutationInfo.impact.timeGeneImpact)
            )
            additionalGeneMutationInfo.archiveMutator.selectGenesByArchive(genes, 1.0/2, additionalGeneMutationInfo.targets)
        }else listOf(date, time)

        val selected = randomness.choose(selects)
        val selectedImpact = genes?.first { it.first == selected }?.second
        selected.standardMutation(randomness, apc, allGenes, true,
                additionalGeneMutationInfo.copyFoInnerGene(selectedImpact))
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is DateTimeGene){
                log.warn("original ({}) should be DateTimeGene", original::class.java.simpleName)
                return
            }
            if (mutated !is DateTimeGene){
                log.warn("mutated ({}) should be DateTimeGene", mutated::class.java.simpleName)
                return
            }

            if (!mutated.date.containsSameValueAs(original.date)){
                date.archiveMutationUpdate(original.date, mutated.date, doesCurrentBetter, archiveMutator)
            }
            if (!mutated.time.containsSameValueAs(original.time))
                time.archiveMutationUpdate(original.time, mutated.time, doesCurrentBetter, archiveMutator)
        }
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