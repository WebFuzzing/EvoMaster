package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene


// Encoding IntegerGene, DoubleGene, EnumGene, and BooleanGene of a RestCallAction into a double vector
fun inputEncoder(input: RestCallAction): List<Double> {

    // Create enum encodings
    val enumEncoding = mutableMapOf<String, Map<String, Int>>()
    for (parameter in input.parameters) {
        val gene = parameter.primaryGene()
        if (gene is EnumGene<*>) {
            val values = gene.values
                .map { it.toString() }
                .filter { it != "EVOMASTER" } //excluding "EVOMASTER" as it appears in Enum lists
            enumEncoding[parameter.name] = values.withIndex().associate { it.value to it.index }
        }
    }

    val encodedFeatures = mutableListOf<Double>()
    for (gene in input.seeTopGenes()) {
        when (gene) {
            is IntegerGene -> {
                encodedFeatures.add(gene.value.toDouble())
            }
            is DoubleGene -> {
                encodedFeatures.add(gene.value.toInt().toDouble()) // or use rounding logic if needed
            }
            is BooleanGene -> {
                val boolVal = gene.getValueAsRawString().toBoolean()
                encodedFeatures.add(if (boolVal) 1.0 else 0.0)
            }
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