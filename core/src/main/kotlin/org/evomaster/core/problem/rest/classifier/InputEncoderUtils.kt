package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import kotlin.reflect.KClass

/**
 * Utility object for encoding the genes of a [RestCallAction] into a numerical representation.
 * This is primarily used in AI-based models that require fixed-length numeric feature vectors
 * as input.
 */
class InputEncoderUtils (
    private val action: RestCallAction,
    private val encoderType: EncoderType
) {

    // List of supported Gene types
    private val supportedGeneTypes: Set<KClass<out Gene>> = setOf(
        IntegerGene::class,
        DoubleGene::class,
        BooleanGene::class,
        EnumGene::class,
        StringGene::class,
        LongGene::class
    )

    /** Expose the set of the supported genes */
    fun geneTypes(): Set<KClass<out Gene>> = supportedGeneTypes

    /** Check if a single gene is supported */
    fun isSupported(g: Gene): Boolean =
        supportedGeneTypes.any { it.isInstance(g) }

    /** Check if all genes in a list are supported */
    fun areAllGenesSupported(): Boolean {
        val genes = endPointToGeneList()
        return genes.all { isSupported(it) }
    }

    /**
     * Recursively expand any Gene into a flat list of "leaf" Genes.
     * - If it's an ObjectGene, expand its fixed + additional fields.
     * - Otherwise, return the gene itself.
     * TODO genes like ArrayGene with flexible length are challenging to deal with and need to be addressed later
     */
    fun expandGene(g: Gene): List<Gene> {
        return when (g) {
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
    }

    fun endPointToGeneList() : List<Gene> {
        // Expand all parameters into leaf genes
        val listGenes = mutableListOf<Gene>()
        action.parameters.forEach { p ->
            val g = p.primaryGene().getLeafGene()
            println("Parameter: ${p.name}, Gene: ${g::class.simpleName}")
            listGenes.addAll(expandGene(g))
        }
        listGenes.replaceAll { it.getLeafGene() }

        return listGenes
    }


    /** Encodes supported genes of a `RestCallAction` into a vector of `Double` elements */
    fun encode(): List<Double> {

        // Encoding enum genes as a mapping string -> int
        val enumEncoding = mutableMapOf<String, Map<String, Int>>()
        for (parameter in action.parameters.filterIsInstance<QueryParam>().sortedBy { it.name }) {
            val gene = parameter.primaryGene().getLeafGene()
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
                /**
                 * Interpreting StringGene as a boolean, based on whether it is empty or not.
                 * TODO It would be more appropriate to represent it as an Enum based on e.g., its length or having char or not, etc.
                 */
                is StringGene -> {
                    rawEncodedFeatures.add(if (gene.value.isNotEmpty()) 1.0 else 0.0 ?: -1e6)
                }
                is BooleanGene -> {
                    rawEncodedFeatures.add(if (gene.value) 1.0 else 0.0 ?: -1e6)
                }
                is EnumGene<*> -> {
                    val paramName = gene.name
                    val raw = gene.getValueAsRawString()
                    val encoded = enumEncoding[paramName]?.get(raw) ?: -1e6
                    rawEncodedFeatures.add(encoded.toDouble())
                }
            }
        }

        // TODO Normalization step should be defined in the config, as normalization may either
        //  weaken or strengthen the classification performance
        val mean = rawEncodedFeatures.average()
        val stdDev = kotlin.math.sqrt(rawEncodedFeatures.map { (it - mean) * (it - mean) }.average())

        return when (encoderType) {
            EncoderType.RAW -> rawEncodedFeatures

            EncoderType.NORMAL ->
                if (stdDev == 0.0) {
                    List(rawEncodedFeatures.size) { 0.0 }
                } else {
                    rawEncodedFeatures.map { (it - mean) / stdDev }
                }

            EncoderType.UNIT_NORMAL ->
                if (rawEncodedFeatures.all { it == 0.0 }) {
                    // if vector is zero, return [1.0, 0.0, ..., 0.0]
                    List(rawEncodedFeatures.size) { i -> if (i == 0) 1.0 else 0.0 }
                } else {
                    val norm = kotlin.math.sqrt(rawEncodedFeatures.sumOf { it * it })
                    rawEncodedFeatures.map { it / norm }
                }
        }

    }
}
