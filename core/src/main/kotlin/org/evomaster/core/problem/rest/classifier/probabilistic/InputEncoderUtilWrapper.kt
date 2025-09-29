package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.BigIntegerGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.FloatingPointNumberGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.IntegralNumberGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.numeric.NumberGene
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
        FloatGene::class,
        FloatingPointNumberGene::class,
        LongGene::class,
        BigDecimalGene::class,
        BigIntegerGene::class,
        IntegralNumberGene::class,
        NumberGene::class,
        BooleanGene::class,
        EnumGene::class,
        StringGene::class,
        ArrayGene::class
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


    /**
     * Encode the current endpoint's gene values into a numeric feature vector.
     *
     *  - A sentinel value (-1e6) is used for missing or null-like cases
     *
     * Each gene is converted to a Double according to its type:
     *  - Numeric genes (e.g., IntegerGene, DoubleGene, FloatGene, LongGene) → their numeric value as Double
     *  - StringGene → sentinel if blank, otherwise 1.0 (presence indicator)
     *  - BooleanGene → 1.0 for true, 0.0 for false
     *  - EnumGene → index of the chosen enum value (ignoring "EVOMASTER"), or sentinel if not found
     *  - ArrayGene → number of non-null and non-empty elements in the array
     *
     * This encoding produces a fixed-length feature vector of doubles
     * that can be consumed by AI models.
     *
     * @return a list of doubles representing the encoded feature vector
     */
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
                /** Handle numeric gene types by converting their value to Double */
                is IntegerGene, is DoubleGene, is FloatGene, is LongGene,
                is BigDecimalGene, is BigIntegerGene, is IntegralNumberGene<*>,
                is FloatingPointNumberGene<*>, is NumberGene<*> -> {
                    rawEncodedFeatures.add((leaf as NumberGene<*>).value.toDouble())
                }
                /**
                 * Encode a StringGene as a binary presence indicator:
                 * - 1.0 if the string is non-blank
                 * - sentinel if the string is blank (treated as missing)
                 */
                is StringGene -> {
                    rawEncodedFeatures.add(if (leaf.value.isBlank()) sentinel else 1.0)
                }
                is BooleanGene -> {
                    rawEncodedFeatures.add(if (leaf.value) 1.0 else 0.0)
                }
                /**
                 * Encode an EnumGene by mapping its selected value to
                 * the index of that value in the list of allowed enum values
                 * (excluding the reserved "EVOMASTER" string).
                 * If the value is not found, use the sentinel.
                 */
                is EnumGene<*> -> {
                    val raw = leaf.getValueAsRawString()
                    val values = leaf.values.map { it.toString() }.filter { it != "EVOMASTER" }
                    val idx = values.indexOf(raw)
                    rawEncodedFeatures.add(if (idx >= 0) idx.toDouble() else sentinel)
                }
                /**
                 * Encode an ArrayGene by counting how many of its elements
                 * are non-null / non-empty (i.e., contain meaningful data).
                 * This reduces the array to a single numeric feature representing
                 * its effective size.
                 */
                is ArrayGene<*> -> {
                    val count = leaf.getViewOfElements()
                        .count { e -> e.getValueAsPrintableString().isNotBlank() }
                    rawEncodedFeatures.add(count.toDouble())
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