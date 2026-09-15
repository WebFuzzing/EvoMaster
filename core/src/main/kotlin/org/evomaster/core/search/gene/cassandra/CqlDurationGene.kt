package org.evomaster.core.search.gene.cassandra

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * A value of the Cassandra "duration" type, which is composed of a number of months, a number of
 * days, and a number of nanoseconds, kept apart from each other rather than reduced to a single
 * amount of time, as the length of a month and of a day both depend on the date they are counted
 * from.
 *
 * The three amounts share a single sign, instead of having one each: a duration literal is written
 * with at most one leading "-", which applies to the whole value, so a duration mixing signs has no
 * representation in CQL.
 *
 * Note that the representation is not unique, as all the amounts being zero and [negative] being
 * true renders "-0mo0d0ns", ie the same value as the positive zero duration spelled differently.
 */
class CqlDurationGene(
    name: String,
    val months: IntegerGene = IntegerGene("months", min = 0),
    val days: IntegerGene = IntegerGene("days", min = 0),
    val nanos: LongGene = LongGene("nanos", min = 0),
    /**
     * Whether the duration is negative, ie the sign shared by the three amounts it is composed of.
     * Explicitly defaulted to false, as [BooleanGene] defaults to true.
     */
    val negative: BooleanGene = BooleanGene("negative", false)
) : CompositeFixedGene(name, mutableListOf(months, days, nanos, negative)) {

    override fun copyContent(): Gene = CqlDurationGene(
        name,
        months.copy() as IntegerGene,
        days.copy() as IntegerGene,
        nanos.copy() as LongGene,
        negative.copy() as BooleanGene
    )

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        months.randomize(randomness, tryToForceNewValue)
        days.randomize(randomness, tryToForceNewValue)
        nanos.randomize(randomness, tryToForceNewValue)
        negative.randomize(randomness, tryToForceNewValue)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\"${getValueAsRawString()}\""
    }

    /**
     * @return the duration as it is written in a CQL statement, in the standard Cassandra format.
     * All three amounts are always written, so that the literal is well formed even when they are
     * all zero.
     */
    override fun getValueAsRawString(): String {
        val sign = if (negative.value) "-" else ""
        return "$sign${months.value}mo${days.value}d${nanos.value}ns"
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is CqlDurationGene) {
            return false
        }

        return this.months.unsafeCopyValueFrom(other.months)
                && this.days.unsafeCopyValueFrom(other.days)
                && this.nanos.unsafeCopyValueFrom(other.nanos)
                && this.negative.unsafeCopyValueFrom(other.negative)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is CqlDurationGene) {
            return false
        }

        return this.months.containsSameValueAs(other.months)
                && this.days.containsSameValueAs(other.days)
                && this.nanos.containsSameValueAs(other.nanos)
                && this.negative.containsSameValueAs(other.negative)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }
}