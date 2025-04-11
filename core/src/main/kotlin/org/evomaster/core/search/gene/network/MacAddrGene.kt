package org.evomaster.core.search.gene.network

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * https://www.postgresql.org/docs/14/datatype-net-types.html#DATATYPE-MACADDR
 * Gene type for 6 and 8 byte MAC addresses.
 */
class MacAddrGene(
    name: String,
    numberOfOctets: Int = MACADDR6_SIZE,
    private val octets: List<IntegerGene> = List(numberOfOctets)
        { i -> IntegerGene("b$i", min = 0, max = 255) }
) : CompositeFixedGene(name, octets.toMutableList()) {

    companion object {

        const val MACADDR6_SIZE = 6

        const val MACADDR8_SIZE = 8

        val log: Logger = LoggerFactory.getLogger(MacAddrGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        octets.forEach { it.randomize(randomness, tryToForceNewValue) }
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
        return octets
                .map { String.format("%02X", it.value) }
                .joinToString(":")

    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is MacAddrGene -> {
                var result = true
                repeat(octets.size) {
                    result = result && octets[it].setValueBasedOn(gene.octets[it])
                }
                result
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind MacAddrGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is MacAddrGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (octets.size != other.octets.size) {
            throw IllegalArgumentException(
                    "cannot bind MacAddrGene${octets.size} with MacAddrGene${other.octets.size}"
            )
        }

        return updateValueOnlyIfValid(
            {
                var ok = true
                repeat(octets.size) {
                    ok = ok && octets[it].copyValueFrom(other.octets[it])
                }
                ok
            }, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is MacAddrGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (octets.size != other.octets.size) {
            return false
        }
        var result = true
        repeat(octets.size) {
            result = result && octets[it].containsSameValueAs(other.octets[it])
        }
        return result
    }

    fun size() = octets.size

    override fun copyContent() = MacAddrGene(name, numberOfOctets = octets.size, octets.map { it.copy() as IntegerGene }.toList())

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}