package org.evomaster.core.search.gene.uri

import org.evomaster.core.Lazy
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import java.net.URI


/**
 *  https://en.wikipedia.org/wiki/Data_URI_scheme
 */
class UriDataGene(
    name: String,
    //TODO could support more types
    val type : EnumGene<String> = EnumGene("type", listOf("","text/plain")),
    val base64 : BooleanGene = BooleanGene("base64"),
    //TODO here the data would depend on type. simplicity just encode into base64 regardless
    val data : Base64StringGene = Base64StringGene("data")
    //TODO could have params k=v here as well
    )  : CompositeFixedGene(name, mutableListOf(type,base64,data)){


    override fun copyContent(): Gene {
        return UriDataGene(
            name,
            type.copy() as EnumGene<String>,
            base64.copy() as BooleanGene,
            data.copy() as Base64StringGene
        )
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        //FIXME URI implementation in Java is broken
        return try{ URI(getValueAsRawString()); true}catch (e: Exception){false}
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        getViewOfChildren().forEach { it.randomize(randomness, tryToForceNewValue) }
    }




    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {

        val t = type.getValueAsRawString()
        val b64 = if(base64.value) ";base64" else ""
        val d = data.getValueAsRawString()

        return "data:$t$b64,$d"
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is UriDataGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        val current = copy()

        return updateValueOnlyIfValid(
            {type.copyValueFrom(other.type) && base64.copyValueFrom(other.base64) && data.copyValueFrom(other.data)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is UriDataGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return type.containsSameValueAs(other.type)
                && base64.containsSameValueAs(other.base64)
                && data.containsSameValueAs(other.data)
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        return false
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