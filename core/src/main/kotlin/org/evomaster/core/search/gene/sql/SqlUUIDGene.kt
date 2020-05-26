package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.sql.SqlUUIDGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * The data type uuid stores Universally Unique Identifiers (UUID) as defined by RFC 4122, ISO/IEC 9834-8:2005,
 * and related standards. (Some systems refer to this data type as a globally unique identifier, or GUID, instead.)
 *
 * https://www.postgresql.org/docs/9.1/datatype-uuid.html
 */
class SqlUUIDGene(
        name: String,
        val mostSigBits: LongGene = LongGene("mostSigBits", 0L),
        val leastSigBits: LongGene = LongGene("leastSigBits", 0L)
) : Gene(name) {

    override fun copy(): Gene = SqlUUIDGene(
            name,
            mostSigBits.copy() as LongGene,
            leastSigBits.copy() as LongGene
    )

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlUUIDGene::class.java)
    }
    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        mostSigBits.randomize(randomness, forceNewValue, allGenes)
        leastSigBits.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) {
        if (!enableAdaptiveGeneMutation){
            val gene = randomness.choose(listOf(mostSigBits, leastSigBits))
            gene.standardMutation(randomness, apc, allGenes)
        }else{
            additionalGeneMutationInfo?:throw IllegalArgumentException("additionalGeneMutationInfo should not be null when enable adaptive gene mutation")
            archiveMutation(randomness, allGenes, apc, additionalGeneMutationInfo)
        }

    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        // https://www.postgresql.org/docs/9.1/datatype-uuid.html
        return getValueAsUUID().toString()
    }

    fun getValueAsUUID(): UUID = UUID(mostSigBits.value, leastSigBits.value)

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlUUIDGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.mostSigBits.copyValueFrom(other.mostSigBits)
        this.leastSigBits.copyValueFrom(other.leastSigBits)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlUUIDGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.mostSigBits.containsSameValueAs(other.mostSigBits)
                && this.leastSigBits.containsSameValueAs(other.leastSigBits)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(mostSigBits.flatView(excludePredicate))
                    .plus(leastSigBits.flatView(excludePredicate))
    }

    private fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl,
                                additionalGeneMutationInfo: AdditionalGeneMutationInfo) {

        var genes : List<Pair<Gene, GeneImpact>>? = null

        val selects =  if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlUUIDGeneImpact){
            genes = listOf(
                    Pair(leastSigBits, additionalGeneMutationInfo.impact.leastSigBitsImpact),
                    Pair(leastSigBits , additionalGeneMutationInfo.impact.mostSigBitsImpact)
            )
            additionalGeneMutationInfo.archiveMutator.selectGenesByArchive(genes, 1.0/2, additionalGeneMutationInfo.targets)
        }else
            listOf(leastSigBits, leastSigBits)

        val selected = randomness.choose(if (selects.isNotEmpty()) selects else listOf(leastSigBits, leastSigBits))
        val selectedImpact = genes?.first { it.first == selected }?.second
        selected.standardMutation(randomness,  apc,allGenes,true , additionalGeneMutationInfo.copyFoInnerGene(selectedImpact))

    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is SqlUUIDGene){
                log.warn("original ({}) should be SqlUUIDGene", original::class.java.simpleName)
                return
            }
            if (mutated !is SqlUUIDGene){
                log.warn("mutated ({}) should be SqlUUIDGene", mutated::class.java.simpleName)
                return
            }

            if (!mutated.leastSigBits.containsSameValueAs(original.leastSigBits)){
                leastSigBits.archiveMutationUpdate(original.leastSigBits, mutated.leastSigBits, doesCurrentBetter, archiveMutator)
            }
            if (!mutated.mostSigBits.containsSameValueAs(original.mostSigBits)){
                mostSigBits.archiveMutationUpdate(original.mostSigBits, mutated.mostSigBits, doesCurrentBetter, archiveMutator)
            }
        }
    }

    override fun reachOptimal(): Boolean {
        return leastSigBits.reachOptimal() && mostSigBits.reachOptimal()
    }


}