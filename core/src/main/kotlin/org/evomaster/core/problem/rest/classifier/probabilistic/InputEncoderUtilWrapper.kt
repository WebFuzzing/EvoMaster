package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import kotlin.math.sqrt
import kotlin.reflect.KClass

/**
 * Utility object for encoding the genes of a [org.evomaster.core.problem.rest.data.RestCallAction] into a numerical representation.
 * This is primarily used in AI-based models that require fixed-length numeric feature vectors
 * as input.
 */
class InputEncoderUtilWrapper(
    private val action: RestCallAction,
    private val encoderType: EMConfig.EncoderType?
) {

    private val supportedGeneTypes: Set<KClass<out Gene>> = setOf(
        IntegerGene::class,
        DoubleGene::class,
        BooleanGene::class,
        EnumGene::class,
        StringGene::class
    )

    fun isSupported(g: Gene): Boolean =
        supportedGeneTypes.any { it.isInstance(g) }

    fun areAllGenesSupported(): Boolean {
        val leafs = endPointToGeneList().map { gene -> gene.getLeafGene() }
        return leafs.all { isSupported(it) }
    }

    fun expandGene(g: Gene): List<Gene> = when (g) {
        is ObjectGene -> {
            val expanded = mutableListOf<Gene>()
            g.fixedFields.forEach { expanded.addAll(expandGene(it)) }
            g.additionalFields?.forEach { pair ->
                expanded.addAll(expandGene(pair.second))
            }
            expanded
        }
        else -> listOf(g)
    }

    fun endPointToGeneList(): List<Gene> {
        val listGenes = mutableListOf<Gene>()
        action.parameters
            .filter { p ->
                val name = p.name
                name != TaintInputName.EXTRA_PARAM_TAINT &&
                        name != TaintInputName.EXTRA_HEADER_TAINT
            }
            .forEach { p ->
                val g = p.primaryGene()
                listGenes.addAll(expandGene(g))
            }
        return listGenes
    }


    fun encode(): List<Double> {
        val sentinel = -1e6 // for null handling
        val listGenes = endPointToGeneList()
        val rawEncodedFeatures = mutableListOf<Double>()

        for (g in listGenes) {

            if(g.getValueAsPrintableString()==""){
                rawEncodedFeatures.add(sentinel)
                continue
            }

            val leaf = g.getLeafGene()
            when (leaf) {
                is IntegerGene -> {
                    rawEncodedFeatures.add(leaf.value.toDouble())
                }
                is DoubleGene -> {
                    rawEncodedFeatures.add(leaf.value)
                }
                is StringGene -> {
                    rawEncodedFeatures.add(if (leaf.value.isBlank()) sentinel else 1.0)
                }
                is BooleanGene -> {
                    rawEncodedFeatures.add(if (leaf.value) 1.0 else 0.0)
                }
                is EnumGene<*> -> {
                    val raw = leaf.getValueAsRawString()
                    val values = leaf.values.map { it.toString() }.filter { it != "EVOMASTER" }
                    val idx = values.indexOf(raw)
                    rawEncodedFeatures.add(if (idx >= 0) idx.toDouble() else sentinel)
                }

                else -> throw IllegalArgumentException("Unsupported gene type: ${g::class.simpleName}")
            }
        }

        if (rawEncodedFeatures.isEmpty()) {
            // Avoid crash when the input is empty by returning an empty list
            return emptyList()
        }

        // Normalization step
        val mean = rawEncodedFeatures.average()
        val stdDev = sqrt(rawEncodedFeatures.map { (it - mean) * (it - mean) }.average())

        return when (encoderType) {
            EMConfig.EncoderType.RAW -> rawEncodedFeatures
            EMConfig.EncoderType.NORMAL ->
                if (stdDev == 0.0) List(rawEncodedFeatures.size) { 0.0 }
                else rawEncodedFeatures.map { (it - mean) / stdDev }
            EMConfig.EncoderType.UNIT_NORMAL ->
                if (rawEncodedFeatures.all { it == 0.0 })
                    List(rawEncodedFeatures.size) { i -> if (i == 0) 1.0 else 0.0 }
                else {
                    val norm = sqrt(rawEncodedFeatures.sumOf { it * it })
                    rawEncodedFeatures.map { it / norm }
                }

            else -> throw IllegalArgumentException("Unsupported encoder type: $encoderType")
        }
    }


}