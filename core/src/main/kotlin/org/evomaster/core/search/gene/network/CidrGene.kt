package org.evomaster.core.search.gene.network

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * https://www.postgresql.org/docs/14/datatype-net-types.html#DATATYPE-CIDR
 * This gene represents a "Classless Internet Domain Routing" (cidr) addresses.
 *
 * The cidr type holds an IPv4 or IPv6 network specification.
 * Input and output formats follow Classless Internet Domain Routing conventions.
 *
 * The format for specifying networks is address/y where address is the network's
 * lowest address represented as an IPv4 or IPv6 address, and y is the number of bits
 * in the netmask. If y is omitted, it is calculated using assumptions from the older
 * classful network numbering system, except it will be at least large enough to include
 * all of the octets written in the input. It is an error to specify a network address
 * that has bits set to the right of the specified netmask.
 */
class CidrGene(
        name: String,
        private val octets: List<IntegerGene> = List(INET_SIZE)
        { i -> IntegerGene("b$i", min = 0, max = 255) }
) : CompositeFixedGene(name, octets.toMutableList()) {

    companion object {
        const val INET_SIZE = 4
        val log: Logger = LoggerFactory.getLogger(CidrGene::class.java)
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
    ): String = "\"${getValueAsRawString()}\""

    override fun getValueAsRawString() = this.octets
            .map { it.value }
            .joinToString(".")




    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is CidrGene -> {
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
        if (other !is CidrGene) {
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
        if (other !is CidrGene) {
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

    override fun copyContent() = CidrGene(name, octets.map { it.copy() as IntegerGene }.toList())

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}