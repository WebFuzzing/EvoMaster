package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.Randomness
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

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        mostSigBits.randomize(randomness, forceNewValue, allGenes)
        leastSigBits.randomize(randomness, forceNewValue, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
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


}