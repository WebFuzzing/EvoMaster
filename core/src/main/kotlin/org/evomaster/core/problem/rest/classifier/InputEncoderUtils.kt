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

    /** Encodes `IntegerGene`, `DoubleGene`, `EnumGene`, and `BooleanGene` of a `RestCallAction` into a vector of `Double` elements */
    fun encode(input: RestCallAction): List<Double> {

        // Create enum encodings
        val enumEncoding = mutableMapOf<String, Map<String, Int>>()
        for (parameter in input.parameters.filterIsInstance<QueryParam>().sortedBy { it.name }) {
            val gene = parameter.primaryGene()
            if (gene is EnumGene<*>) {
                val values = gene.values
                    .map { it.toString() }
                    .filter { it != "EVOMASTER" } // excluding "EVOMASTER"
                enumEncoding[parameter.name] = values.withIndex().associate { it.value to it.index }
            }
        }
        //TODO: We need to handle other types, eg, OptionalGene, and other types of numerics (eg Long and Float)
        val encodedFeatures = mutableListOf<Double>()
        for (gene in input.seeTopGenes()) {
            when (gene) {
                is IntegerGene -> encodedFeatures.add(gene.value.toDouble())
                is DoubleGene -> encodedFeatures.add(gene.value)
                is BooleanGene -> encodedFeatures.add(if (gene.getValueAsRawString().toBoolean()) 1.0 else 0.0)
                is EnumGene<*> -> {
                    val paramName = gene.name
                    val rawValue = gene.getValueAsRawString()
                    val encoded = enumEncoding[paramName]?.get(rawValue) ?: -1
                    encodedFeatures.add(encoded.toDouble())
                }
            }
        }

        return encodedFeatures
    }
}
