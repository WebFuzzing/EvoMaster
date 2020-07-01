package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.impact.impactInfoCollection.GeneImpact
import org.evomaster.core.search.impact.impactInfoCollection.sql.SqlUUIDGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
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

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return listOf(mostSigBits, leastSigBits)
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlUUIDGeneImpact){
            val maps = mapOf<Gene, GeneImpact>(
                    mostSigBits to additionalGeneMutationInfo.impact.mostSigBitsImpact,
                    leastSigBits to additionalGeneMutationInfo.impact.leastSigBitsImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it)) }
        }
        throw IllegalArgumentException("impact is null or not DateTimeGeneImpact")
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


    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, EvaluatedMutation>, archiveMutator: ArchiveGeneMutator) {
        if (original !is SqlUUIDGene){
            log.warn("original ({}) should be SqlUUIDGene", original::class.java.simpleName)
            return
        }
        if (mutated !is SqlUUIDGene){
            log.warn("mutated ({}) should be SqlUUIDGene", mutated::class.java.simpleName)
            return
        }

        if (!mutated.leastSigBits.containsSameValueAs(original.leastSigBits)){
            leastSigBits.archiveMutationUpdate(original.leastSigBits, mutated.leastSigBits, targetsEvaluated, archiveMutator)
        }
        if (!mutated.mostSigBits.containsSameValueAs(original.mostSigBits)){
            mostSigBits.archiveMutationUpdate(original.mostSigBits, mutated.mostSigBits, targetsEvaluated, archiveMutator)
        }
    }

    override fun reachOptimal(targets: Set<Int>): Boolean {
        return leastSigBits.reachOptimal(targets) && mostSigBits.reachOptimal(targets)
    }


}