package org.evomaster.core.search.gene.uri

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

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is UriDataGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return type.containsSameValueAs(other.type)
                && base64.containsSameValueAs(other.base64)
                && data.containsSameValueAs(other.data)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is UriDataGene) {
            return false
        }

        return type.unsafeCopyValueFrom(other.type)
                && base64.unsafeCopyValueFrom(other.base64)
                && data.unsafeCopyValueFrom(other.data)
    }


    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    @Deprecated("Do not call directly outside this package. Call setFromStringValue")
    override fun unsafeSetFromStringValue(value: String): Boolean {
        // TODO: Charset value is not handled in UriDataGene.
        //  If the encoded string uses a different Charset test will fail,
        //  since the Base64StringGene.unsafeSetFromStringValue() use UTF_8 to decode the value.
        return try {
            val uri = URI(value)

            if (uri.scheme == "data") {
                val uriParts = uri.schemeSpecificPart
                val parts = uriParts.split(",", limit = 2)
                val metadata = parts[0].split(";")
                val b64Value = metadata[2].equals("base64", ignoreCase = true)

                val t = type.unsafeSetFromStringValue(metadata[0])
                val b64 = base64.unsafeSetFromStringValue(b64Value.toString())
                val data = data.unsafeSetFromStringValue(parts[1])
                t && b64 && data
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

}
