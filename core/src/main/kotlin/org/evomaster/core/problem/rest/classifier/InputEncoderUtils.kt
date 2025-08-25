package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene

/**
 * Utility object for encoding the genes of a [RestCallAction] into a numerical representation.
 * This is primarily used in AI-based models that require fixed-length numeric feature vectors
 * as input.
 */
object InputEncoderUtils {

    data class EncodedFeatures(
        val rawEncodedFeatures: List<Double>,
        val normalizedEncodedFeatures: List<Double>
    )

    /** Encodes `IntegerGene`, `DoubleGene`, `EnumGene`, and `BooleanGene` of a `RestCallAction` into a vector of `Double` elements */
    fun encode(action: RestCallAction): EncodedFeatures {

        // Encoding enum genes as a mapping string -> int
        val enumEncoding = mutableMapOf<String, Map<String, Int>>()
        for (parameter in action.parameters.filterIsInstance<QueryParam>().sortedBy { it.name }) {
            val gene = parameter.gene.getLeafGene()
            if (gene is EnumGene<*>) {
                val values = gene.values
                    .map { it.toString() }
                    .filter { it != "EVOMASTER" } // excluding "EVOMASTER"
                enumEncoding[parameter.name] = values.withIndex().associate { it.value to it.index }
            }
        }
        //TODO: We need to handle other types, eg, OptionalGene, and other types of numerics (eg Long and Float)
        /**
         * The null handling is based on considering null values as -1e6
         */
        val rawEncodedFeatures = mutableListOf<Double>()
        for (gene in action.seeTopGenes()) {
            if(gene.getValueAsRawString()==""){
                rawEncodedFeatures.add(-1e6)
                continue
            }
            val gene=gene.getLeafGene()
            when (gene) {
                is IntegerGene -> rawEncodedFeatures.add(gene.value.toDouble() ?: -1e6)
                is DoubleGene -> rawEncodedFeatures.add(gene.value ?: -1e6)
                is BooleanGene -> {
                    val raw = gene.getValueAsRawString()
                    rawEncodedFeatures.add(if (raw != "" && raw.toBoolean()) 1.0 else 0.0 ?: -1e6)
                }
                is EnumGene<*> -> {
                    val paramName = gene.name
                    val rawValue = gene.getValueAsRawString()
                    val encoded = enumEncoding[paramName]?.get(rawValue) ?: -1e6
                    rawEncodedFeatures.add(encoded.toDouble())
                }
            }
        }

        // TODO Normalization step should be defined in the config, as normalization may either
        //  weaken or strengthen the classification performance
        val mean = rawEncodedFeatures.average()
        val stdDev = kotlin.math.sqrt(rawEncodedFeatures.map { (it - mean) * (it - mean) }.average())

        val normalizedEncodedFeatures: List<Double> =
            if (stdDev == 0.0) {
                List(rawEncodedFeatures.size) { 0.0 }
            } else {
                rawEncodedFeatures.map { (it - mean) / stdDev }
            }

        return EncodedFeatures(
            rawEncodedFeatures = rawEncodedFeatures,
            normalizedEncodedFeatures = normalizedEncodedFeatures
        )

    }
}
