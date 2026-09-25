package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.BigIntegerGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.FloatingPointNumberGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.IntegralNumberGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.StringGene
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sign
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
        ArrayGene::class,
        DateGene::class,
        TimeGene::class,
        DateTimeGene::class,
        RegexGene::class
    )

    /**
     * Represents a mapping between a parameter (with its name and path)
     * and its associated gene object.
     * @property paramName The name of the parameter.
     * @property paramPath A unique identifier for the parameter, representing its hierarchical path (including all its parents).
     * @property gene The gene corresponding to the parameter.
     */
    data class ParamAndGene(
        val paramName: String,
        val paramPath: String,
        val gene: Gene
    )

    fun isSupported(g: Gene): Boolean =
        supportedGeneTypes.any { it.isInstance(g) }

    fun areAllGenesSupported(): Boolean =
        endPointToGeneList().all { isSupported(it.gene.getLeafGene()) }

    fun areAllGenesUnSupported(): Boolean =
        endPointToGeneList().all { !isSupported(it.gene.getLeafGene()) }

    /**
     * Builds a string representing the gene name and all its parents.
     * This string is used as a unique identifier for the gene in the AI models.
     */
    private fun genePath(g: Gene): String {

        val names = mutableListOf<String>()

        var current: Gene? = g

        while (current != null) {
            names.add(current.name)
            current = current.parent as? Gene
        }

        val path = names.reversed()

        return if (path.size > 1)
            path.dropLast(1).joinToString("/") //ignore the last name, which is the repetition of gene itself as its own parent
        else
            path.joinToString("/")

    }

    /**
     * Recursively expands the input gene into a list of its leaf genes.
     * If the input gene is of type [ObjectGene], it will traverse its fixed fields
     * and additional fields to expand and collect all nested leaf genes.
     */
    private fun expandGene(g: Gene): List<Gene> {

        val gene = g.getLeafGene()

        if (gene is ObjectGene) {
            val expanded = mutableListOf<Gene>()
            gene.fixedFields.forEach { expanded.addAll(expandGene(it)) }
            gene.additionalFields?.forEach { pair ->
                expanded.addAll(expandGene(pair.second))
            }
            return expanded
        }

        return listOf(gene)
    }


    /**
     * Associate all parameters' paths with their corresponding encoded numerical values of their parameter.
     * Note that each endpoint may have multiple parameters, but each parameter has a unique path including all its parents.
     */
    fun getAllParamsPathsAndEncodedValues(): Map<String, Double> {

        val paramPaths = endPointToGeneList().map { it.paramPath }
        val encodedValues = encode()

        return paramPaths.zip(encodedValues).toMap()
    }

    /**
     * Converts the endpoint parameters into a list of `ParamAndGene` objects,
     * where each entry represents a parameter, its associated gene, and all the gene's parents.
     */
    fun endPointToGeneList(): List<ParamAndGene> {
        val paramAndGenes = mutableListOf<ParamAndGene>()

        action.parameters
            .filter { p ->
                val name = p.name
                name != TaintInputName.EXTRA_PARAM_TAINT &&
                        name != TaintInputName.EXTRA_HEADER_TAINT
            }
            .forEach { p ->
                val g = p.primaryGene()
                val expanded = expandGene(g)
                expanded.forEach { subGene ->
                    paramAndGenes.add(
                        ParamAndGene(
                            paramName = subGene.name,
                            paramPath = genePath(subGene),
                            gene = subGene
                        )
                    )
                }
            }

        return paramAndGenes
    }

    /**
     * Encodes a string into a single numeric value using a bitmask-style representation.
     *
     * Each string property contributes a unique value:
     * - 1: the string is non-blank
     * - 2: the string contains at least one digit
     * - 4: the string contains at least one letter
     * - 8: the string contains at least one non-alphanumeric character
     *
     * The final encoded value is the sum of the active properties, giving a unique
     * representation for each possible combination.
     *
     * Examples:
     * - "123"     -> 1 + 2 = 3
     * - "abc"     -> 1 + 4 = 5
     * - "abc123"  -> 1 + 2 + 4 = 7
     * - "abc-123" -> 1 + 2 + 4 + 8 = 15
     *
     * If the input string is blank, [sentinel] (e.g., -1e3) is returned.
     */
    private fun encodeString(value: String, sentinel: Double): Double {

        if (value.isBlank()) {
            return sentinel
        }

        var encoded = 1 // non-blank

        if (value.any { it.isDigit() }) {
            encoded += 2
        }

        if (value.any { it.isLetter() }) {
            encoded += 4
        }

        if (value.any { !it.isLetterOrDigit() }) {
            encoded += 8
        }

        return encoded.toDouble()
    }

    /**
     * Encodes an [ArrayGene] into a single numeric value based on the types of
     * its non-empty elements.
     *
     * Empty or blank elements are ignored. Each meaningful element contributes:
     * - StringGene  -> +1
     * - NumericGene -> +100
     * - EnumGene    -> +1000
     *
     * Other gene types do not contribute to the encoded value.
     *
     * If the array contains no meaningful elements, [sentinel] is returned.
     */
    private fun encodeArray(array: ArrayGene<*>, sentinel: Double): Double {

        val elements = array.getViewOfElements()
            .filter { it.getValueAsPrintableString().isNotBlank() }

        if (elements.isEmpty()) {
            return sentinel
        }

        var encoded = elements.size

        elements.forEach { element ->
            when (element.getLeafGene()) {

                is StringGene ->
                    encoded += 10

                is IntegerGene, is DoubleGene, is FloatGene, is LongGene,
                is BigDecimalGene, is BigIntegerGene,
                is IntegralNumberGene<*>,
                is FloatingPointNumberGene<*>,
                is NumberGene<*> ->
                    encoded += 100

                is EnumGene<*> ->
                    encoded += 1000
            }
        }

        return encoded.toDouble()
    }

    /**
     * Encodes the current endpoint's gene values into a numeric feature vector suitable for
     * machine-learning or classification tasks.
     *
     *  - A sentinel value (-1e3) is used for missing, invalid, or null-like cases.
     *    This value is outside the approximate signed-log numeric range [-710, +710].
     *  - A neutral value (0.0) is used for unsupported genes.
     *
     * Each gene is converted to a Double according to its type:
     *  - Numeric genes → signed logarithmic scaling: sign(x) * ln(1 + abs(x))
     *  - StringGene → encoded using character-type bitmask
     *  - RegexGene → encoded from its generated string using the same bitmask
     *  - BooleanGene → 1.0 for true, 0.0 for false
     *  - EnumGene → index of the chosen enum value, excluding "EVOMASTER"
     * - ArrayGene → weighted sum of non-empty elements based on their gene type:
     *  - DateGene → epoch days divided by 100,000
     *  - TimeGene → fraction of a day in [0, 1)
     *  - DateTimeGene → epoch days plus fractional day, divided by 100,000
     *
     * Unsupported genes are encoded using the neutral value.
     */
    fun encode(sentinel: Double = -1e3, neutral: Double = 0.0): List<Double> {
        val listGenes = endPointToGeneList().map { it.gene }
        val rawEncodedFeatures = mutableListOf<Double>()

        for (g in listGenes) {

            if(!isSupported(g)){
                rawEncodedFeatures.add(neutral)
                continue
            }

            if(!g.staticCheckIfImpactPhenotype() || g.getValueAsPrintableString()==""){
                rawEncodedFeatures.add(sentinel)
                continue
            }

            val leaf = g.getLeafGene()
            when (leaf) {
                /**
                 * Handle numeric gene types by converting their value to Double and applying
                 * signed logarithmic scaling.
                 * This prevents very large numeric values from dominating the encoded feature
                 * space while preserving the sign and relative magnitude of the original value.
                 * Non-finite values are encoded as 0.0.
                 */
                is IntegerGene, is DoubleGene, is FloatGene, is LongGene,
                is BigDecimalGene, is BigIntegerGene, is IntegralNumberGene<*>,
                is FloatingPointNumberGene<*>, is NumberGene<*> -> {

                    val value = leaf.value.toDouble()

                    val encodedValue =
                        if (value.isFinite()) {
                            sign(value) * ln(1.0 + abs(value))
                        } else {
                            sentinel
                        }

                    rawEncodedFeatures.add(encodedValue)
                }
                /** Encode based on [encodeString] function*/
                is StringGene -> {
                    rawEncodedFeatures.add(
                        encodeString(leaf.value, sentinel)
                    )
                }
                /** Encode based on [encodeString] function*/
                is RegexGene -> {
                    rawEncodedFeatures.add(
                        encodeString(leaf.getValueAsRawString(), sentinel)
                    )
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
                /** Encode based on [encodeArray] function*/
                is ArrayGene<*> -> {
                    rawEncodedFeatures.add(
                        encodeArray(leaf, sentinel)
                    )
                }
                /**
                 * Date gene encoded as scaled epoch days.
                 */
                is DateGene -> {
                    try {
                        val epochDays = java.time.LocalDate.of(
                            leaf.year.value,
                            leaf.month.value.coerceIn(1, 12),
                            leaf.day.value.coerceIn(1, 28)
                        ).toEpochDay()

                        rawEncodedFeatures.add(epochDays / 100_000.0)
                    } catch (ex: Exception) {
                        rawEncodedFeatures.add(sentinel)
                    }
                }
                /**
                 * Time gene encoded as fraction of a day in [0, 1).
                 */
                is TimeGene -> {
                    try {
                        val fractionOfDay =
                            (leaf.hour.value.coerceIn(0, 23) / 24.0) +
                                    (leaf.minute.value.coerceIn(0, 59) / (24.0 * 60.0)) +
                                    (leaf.second.value.coerceIn(0, 59) / (24.0 * 3600.0))

                        rawEncodedFeatures.add(fractionOfDay)
                    } catch (ex: Exception) {
                        rawEncodedFeatures.add(sentinel)
                    }
                }
                /**
                 * DateTime gene encoded as scaled epoch days plus a scaled fractional-day component.
                 */
                is DateTimeGene -> {
                    try {
                        val epochDays = java.time.LocalDate.of(
                            leaf.date.year.value,
                            leaf.date.month.value.coerceIn(1, 12),
                            leaf.date.day.value.coerceIn(1, 28)
                        ).toEpochDay()

                        val fractionOfDay =
                            (leaf.time.hour.value.coerceIn(0, 23) / 24.0) +
                                    (leaf.time.minute.value.coerceIn(0, 59) / (24.0 * 60.0)) +
                                    (leaf.time.second.value.coerceIn(0, 59) / (24.0 * 3600.0))

                        rawEncodedFeatures.add(
                            (epochDays + fractionOfDay) / 100_000.0
                        )
                    } catch (ex: Exception) {
                        rawEncodedFeatures.add(sentinel)
                    }
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