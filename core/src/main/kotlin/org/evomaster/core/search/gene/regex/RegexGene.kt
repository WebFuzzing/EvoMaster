package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.parser.RegexType
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.regex.RegexGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import java.util.regex.Pattern

/**
 * A gene representing a regular expression (regex).
 */
class RegexGene(
    name: String,
    val disjunctions: DisjunctionListRxGene,
    val sourceRegex : String,
    val regexType: RegexType,
    /**
     * Optional value to use as it is, instead of relying on tree data-structure.
     * the problem here is that we might want a specific value (eg coming from a seeded
     * test), but putting the tree in the right configuration to represent it would be
     * a complex, non-linear problem to solve.
     * so, this is a reasonable workaround
     */
    var fixedValue: String? = null,
    var usingFixedValue: Boolean = false
) : CompositeFixedGene(name, disjunctions) {


    override fun copyContent(): Gene {
        return RegexGene(name, disjunctions.copy() as DisjunctionListRxGene, sourceRegex, regexType, fixedValue, usingFixedValue)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        usingFixedValue = if(fixedValue == null){
            false
        } else {
            randomness.nextBoolean()
        }
        disjunctions.randomize(randomness, tryToForceNewValue)
    }

    @Deprecated("Do not call directly outside this package. Call setFromStringValue")
    override fun unsafeSetFromStringValue(value: String): Boolean {
        usingFixedValue = true
        fixedValue = value
        return true
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{

        if(!usingFixedValue){
            return true
        }

        if(fixedValue == null){
            //if using a fixed value, it must be specified
            return false
        }

        if(regexType == RegexType.JVM){
            val matcher = try{
                Pattern.compile(sourceRegex).matcher(fixedValue!!)
            }catch(e: Exception){
                return false
            }
            return matcher.find()
        }

        //TODO other regex types...
        return true
    }


    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {

        if(fixedValue == null){
            return false
        }

        if(usingFixedValue){
            return true
        }

        return randomness.nextBoolean(0.1)
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if(fixedValue == null){
            return false
        }
        usingFixedValue = !usingFixedValue
        return true
    }

    override fun isMutable(): Boolean {
        return disjunctions.isMutable()
    }


    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is RegexGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(disjunctions))
                throw IllegalArgumentException("mismatched internal gene")
            return listOf(disjunctions to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.listRxGeneImpact, disjunctions))
        }
        throw IllegalArgumentException("mismatched gene impact")
    }


    override fun getValueAsRawString(): String {

        if(usingFixedValue && fixedValue != null){
            return fixedValue!!
        }

        return disjunctions.getValueAsPrintableString(targetFormat = null)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        val rawValue = getValueAsRawString()
        when {
            // TODO Should refactor since this code block is equivalent to StringGene.getValueAsPrintableString()
            /*(targetFormat == null) -> return "\"$rawValue\""
            targetFormat.isKotlin() -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
                    .replace("$", "\\$")
            else -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
             */
            (targetFormat == null) -> return "\"${rawValue}\""
            //"\"${rawValue.replace("\"", "\\\"")}\""
            (mode != null) -> return "\"${GeneUtils.applyEscapes(rawValue, mode, targetFormat)}\""
            else -> return "\"${GeneUtils.applyEscapes(rawValue, GeneUtils.EscapeMode.TEXT ,targetFormat)}\""
        }
    }




    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return getValueAsRawString() == other.getValueAsRawString()
    }



    /**
     * use mutationweight of [disjunctions]
     */
    override fun mutationWeight(): Double = disjunctions.mutationWeight()


    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if(other !is RegexGene){
            return false
        }

        if(other.usingFixedValue && other.fixedValue != null){
            this.fixedValue = other.fixedValue
            this.usingFixedValue = true
            return true
        }

        usingFixedValue = false
        return this.disjunctions.unsafeCopyValueFrom(other.disjunctions)
    }
}
