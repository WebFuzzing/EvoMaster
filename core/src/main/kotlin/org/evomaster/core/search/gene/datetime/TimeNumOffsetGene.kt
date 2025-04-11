package org.evomaster.core.search.gene.datetime

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy


/**
 * Using RFC3339
 *
 * https://datatracker.ietf.org/doc/html/rfc3339#section-5.6
 *
 *    time-hour       = 2DIGIT  ; 00-23
 *    time-minute     = 2DIGIT  ; 00-59
 *    time-numoffset  = ("+" / "-") time-hour ":" time-minute
 *    time-offset     = "Z" / time-numoffset
 *
 *    Note: RFC3339 does NOT put constraints on hour, but Java does, ie, range -18,+18.
 *    Apparently this is based on ISO8601, which RFC3339 "profiles"... but
 *    that document costs money to read... also, it seems currently only -14,+12 is used
 *    in practice in the world
 */
class TimeNumOffsetGene(
  name: String,
  val sign: EnumGene<String> = EnumGene("sign", listOf("-","+"), treatAsNotString = true),
  val hour: IntegerGene = IntegerGene("hour", min = 0, max = 18),
  val minute: IntegerGene = IntegerGene("minute", min = 0, max = 59)
) : CompositeFixedGene(name, listOf(sign, hour, minute)) {

    override fun copyContent(): Gene {
        return TimeNumOffsetGene(
            name,
            sign.copy() as EnumGene<String>,
            hour.copy() as IntegerGene,
            minute.copy() as IntegerGene
        )
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        sign.randomize(randomness, tryToForceNewValue)
        hour.randomize(randomness, tryToForceNewValue)
        minute.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return getValueAsRawString()
    }

    override fun getValueAsRawString(): String {
        return "${sign.getValueAsRawString()}${GeneUtils.padded(hour.value, 2)}:${GeneUtils.padded(minute.value, 2)}"
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is TimeNumOffsetGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            { sign.copyValueFrom(other.sign) && minute.copyValueFrom(other.minute) && hour.copyValueFrom(other.hour) },
            true)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TimeNumOffsetGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return sign.containsSameValueAs(other.sign)
                && hour.containsSameValueAs(other.hour)
                && minute.containsSameValueAs(other.minute)
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        //TODO
        return false
    }
}