package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlUUIDGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * The data type uuid stores Universally Unique Identifiers (UUID) as defined by RFC 4122, ISO/IEC 9834-8:2005,
 * and related standards. (Some systems refer to this data type as a globally unique identifier, or GUID, instead.)
 *
 * https://www.postgresql.org/docs/9.1/datatype-uuid.html
 */
class UUIDGene(
    name: String,
    val mostSigBits: LongGene = LongGene("mostSigBits", 0L),
    val leastSigBits: LongGene = LongGene("leastSigBits", 0L)
) : CompositeFixedGene(name, mutableListOf(mostSigBits, leastSigBits)) {

    override fun copyContent(): Gene = UUIDGene(
        name,
        mostSigBits.copy() as LongGene,
        leastSigBits.copy() as LongGene
    )

    companion object{
        private val log: Logger = LoggerFactory.getLogger(UUIDGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        mostSigBits.randomize(randomness, tryToForceNewValue)
        leastSigBits.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }


    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlUUIDGeneImpact){
            val maps = mapOf<Gene, GeneImpact>(
                    mostSigBits to additionalGeneMutationInfo.impact.mostSigBitsImpact,
                    leastSigBits to additionalGeneMutationInfo.impact.leastSigBitsImpact
            )
            return mwc.selectSubGene(internalGenes, adaptiveWeight = true, targets = additionalGeneMutationInfo.targets, impacts = internalGenes.map { i-> maps.getValue(i) }, individual = null, evi = additionalGeneMutationInfo.evi).map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }
        throw IllegalArgumentException("impact is null or not DateTimeGeneImpact")
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        // https://www.postgresql.org/docs/9.1/datatype-uuid.html
        return getValueAsUUID().toString()
    }

    fun getValueAsUUID(): UUID = UUID(mostSigBits.value, leastSigBits.value)

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is UUIDGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.mostSigBits.copyValueFrom(other.mostSigBits) && this.leastSigBits.copyValueFrom(other.leastSigBits)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is UUIDGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.mostSigBits.containsSameValueAs(other.mostSigBits)
                && this.leastSigBits.containsSameValueAs(other.leastSigBits)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when{
            gene is UUIDGene ->{
                mostSigBits.setValueBasedOn(gene.mostSigBits) && leastSigBits.setValueBasedOn(gene.leastSigBits)
            }
            gene is StringGene && gene.getSpecializationGene() != null ->{
                setValueBasedOn(gene.getSpecializationGene()!!)
            }
            else->{
                LoggingUtil.uniqueWarn(log, "cannot bind UUIDGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

}