package org.evomaster.core.problem.rest.seeding

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.swagger.v3.oas.models.OpenAPI
import org.apache.commons.codec.binary.Base64
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * Parsers allow to transform a set of test cases in a specific format into a set
 * of test cases that EvoMaster can handle. To this end, they need two key components:
 * the set of default actions (i.e., actions representing single calls to each API
 * operation) and the Swagger specification.
 */
abstract class AbstractParser(
        protected val defaultRestCallActions: List<RestCallAction>,
        protected val swagger: OpenAPI
) : Parser {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractParser::class.java)
        private const val WARN_REQ_PARAM_MISSING = "Required parameter {} was not found in a seeded request. Ignoring and keeping the parameter..."
        private const val WARN_WRONG_VALUE = "Attempt to set {} parameter {} with non-{} value {}. Ignoring and keeping the parameter the same..."
        private const val WARN_WRONG_DATE_TIME = "Attempt to set {} parameter {} with invalid {} {}. Ignoring and keeping the parameter the same..."
        private const val WARN_WRONG_JSON = "Failed to parse parameter {} as JSON"
    }

    /**
     * Given a parameter value, update a gene and all its subgenes (e.g., StringGene
     * within OptionalGene) based on it. If the parameter value is null, it means
     * that it was not present in the original parsed request.
     *
     * @return true if there wasn't any problems updating the gene, false otherwise
     * (e.g., trying to assign a string to an integer parameter).
     */
    fun updateGenesRecursivelyWithParameterValue(gene: Gene, paramName: String, paramValue: String?): Boolean {
        // Optional parameter not present in request
        if (paramValue == null) {
            return if (gene is OptionalGene) {
                gene.isActive = false
                true
            } else
                logWarningAndReturnFalse(WARN_REQ_PARAM_MISSING, paramName)
        }

        return when (gene) {

            // Basic data type genes
            is StringGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is BooleanGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is DoubleGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is FloatGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is IntegerGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is LongGene -> updateGeneWithParameterValue(gene, paramName, paramValue)

            // Non-basic data type but terminal genes
            is Base64StringGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is EnumGene<*> -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is DateGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is TimeGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is DateTimeGene -> updateGeneWithParameterValue(gene, paramName, paramValue)

            // Non-terminal genes (iterate over children)
            is OptionalGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is DisruptiveGene<*> -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is ArrayGene<*> -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is ObjectGene -> updateGeneWithParameterValue(gene, paramName, paramValue)
            is MapGene<*> -> updateGeneWithParameterValue(gene, paramName, paramValue)

            else -> {
                // ImmutableDataHolderGene should never happen
                throw IllegalStateException("Unexpected gene found in RestCallAction")
            }
        }
    }

    protected fun updateGeneWithParameterValue(gene: StringGene, paramName: String, paramValue: String): Boolean {
        gene.value = paramValue
        return true
    }

    protected fun updateGeneWithParameterValue(gene: BooleanGene, paramName: String, paramValue: String): Boolean {
        when (paramValue) {
            "true" -> gene.value = true
            "false" -> gene.value = false
            else -> return logWarningAndReturnFalse(WARN_WRONG_VALUE, "boolean", paramName, "boolean", paramValue)
        }

        return true
    }

    protected fun updateGeneWithParameterValue(gene: DoubleGene, paramName: String, paramValue: String): Boolean {
        try {
            gene.value = paramValue.toDouble()
        } catch (e: NumberFormatException) {
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "double", paramName, "double", paramValue)
        }

        return true
    }

    protected fun updateGeneWithParameterValue(gene: FloatGene, paramName: String, paramValue: String): Boolean {
        try {
            gene.value = paramValue.toFloat()
        } catch (e: NumberFormatException) {
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "float", paramName, "float", paramValue)
        }

        return true
    }

    protected fun updateGeneWithParameterValue(gene: IntegerGene, paramName: String, paramValue: String): Boolean {
        try {
            gene.value = paramValue.toInt()
        } catch (e: NumberFormatException) {
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "integer", paramName, "integer", paramValue)
        }

        return true
    }

    protected fun updateGeneWithParameterValue(gene: LongGene, paramName: String, paramValue: String): Boolean {
        try {
            gene.value = paramValue.toLong()
        } catch (e: NumberFormatException) {
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "long", paramName, "long", paramValue)
        }

        return true
    }

    protected fun updateGeneWithParameterValue(gene: Base64StringGene, paramName: String, paramValue: String): Boolean {
        if (Base64.isBase64(paramValue))
            gene.data.value = paramValue
        else
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "base64", paramName, "base64", paramValue)

        return true
    }

    protected fun updateGeneWithParameterValue(gene: EnumGene<*>, paramName: String, paramValue: String): Boolean {
        if (gene.values.map { it.toString() }.contains(paramValue))
            gene.index = gene.values.map { it.toString() }.indexOf(paramValue)
        else
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "enum", paramName, "enum", paramValue)

        return true
    }

    protected fun updateGeneWithParameterValue(gene: DateGene, paramName: String, paramValue: String): Boolean {
        // TODO: Maybe support more date formats, although Swagger only supports this
        /*
            Here we don't want to parse the date, since we may use bad dates intentionally.
            Instead, we check the format with a regular expression and see if year, month
            and day are between EvoMaster genes' constraints.
         */
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (paramValue.matches(dateRegex)) {
            val year = paramValue.substring(0, 4).toInt()
            val month = paramValue.substring(5, 7).toInt()
            val day = paramValue.substring(8).toInt()
            if (year <= DateGene.MAX_YEAR && year >= DateGene.MIN_YEAR
                    && month <= DateGene.MAX_MONTH && month >= DateGene.MIN_MONTH
                    && day <= DateGene.MAX_DAY && day >= DateGene.MIN_DAY) {
                gene.year.value = year
                gene.month.value = month
                gene.day.value = day
            } else
                return logWarningAndReturnFalse(WARN_WRONG_DATE_TIME, "date", paramName, "date", paramValue)
        } else
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "date", paramName, "date", paramValue)

        return true
    }

    protected fun updateGeneWithParameterValue(gene: TimeGene, paramName: String, paramValue: String): Boolean {
        /*
            TODO: Same comment as in DateGene
         */
        val timeRegex = Regex("^\\d{2}:\\d{2}:\\d{2}(.\\d{3}Z)?$")
        if (paramValue.matches(timeRegex)) {
            val hour = paramValue.substring(0, 2).toInt()
            val minute = paramValue.substring(3, 5).toInt()
            val second = paramValue.substring(6, 8).toInt()
            if (hour <= TimeGene.MAX_HOUR && hour >= TimeGene.MIN_HOUR
                    && minute <= TimeGene.MAX_MINUTE && minute >= TimeGene.MIN_MINUTE
                    && second <= TimeGene.MAX_SECOND && second >= TimeGene.MIN_SECOND) {
                gene.hour.value = hour
                gene.minute.value = minute
                gene.second.value = second
            } else
                return logWarningAndReturnFalse(WARN_WRONG_DATE_TIME, "time", paramName, "time", paramValue)
        } else
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "time", paramName, "time", paramValue)

        return true
    }

    protected fun updateGeneWithParameterValue(gene: DateTimeGene, paramName: String, paramValue: String): Boolean {
        /*
            TODO: Same comment as in Date and Time genes
         */
        val dateTimeRegex = "^(\\d{4}-\\d{2}-\\d{2})[ T](\\d{2}:\\d{2}:\\d{2}(.\\d{3}Z)?)$"
        if (paramValue.matches(Regex(dateTimeRegex))) {
            val matcher = Pattern
                    .compile(dateTimeRegex)
                    .matcher(paramValue)
            matcher.find()
            updateGeneWithParameterValue(gene.date, paramName, matcher.group(1))
            updateGeneWithParameterValue(gene.time, paramName, matcher.group(2))
        } else
            return logWarningAndReturnFalse(WARN_WRONG_VALUE, "date-time", paramName, "date-time", paramValue)

        return true
    }

    protected fun updateGeneWithParameterValue(gene: OptionalGene, paramName: String, paramValue: String): Boolean {
        return updateGenesRecursivelyWithParameterValue(gene.gene, gene.name, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: DisruptiveGene<*>, paramName: String, paramValue: String): Boolean {
        return updateGenesRecursivelyWithParameterValue(gene.gene, gene.gene.name, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: ArrayGene<*>, paramName: String, paramValue: String): Boolean {
        /*
            Here we may have an array as a query/header/path/cookie parameter
            or inside a request body. In the first case, we assume the values
            are comma-separated.

            TODO: Support more serialization options. This involves not only different
             separators other than ',' (e.g., '|' and ' '), but also different parsing
             of the request. For example, in query parameters it is possible to have
             arrays expressed as "/endpoint?param=a&param=b&param=c", which is equal
             to "/endpoint?param=a,b,c". However, for this, it would be necessary to
             look for the serialization 'style' and 'explode' properties in the Swagger.
             Reference: https://swagger.io/docs/specification/serialization/

            TODO: Support nested objects and arrays in non-body parameters
         */

        var res = true

        gene.elements.clear()

        val elements = try {
            Gson().fromJson(paramValue, ArrayList::class.java)
        } catch (ex: JsonSyntaxException) { // Value is not within body, but comma separated
            paramValue.split(',')
        }

        if (elements.size > ArrayGene.MAX_SIZE)
            gene.maxSize = elements.size
        elements.forEach { element ->
            val elementGene = gene.template.copy()
            elementGene.parent = gene
            if (updateGenesRecursivelyWithParameterValue(elementGene, elementGene.name, element as String?))
                addGeneToArrayGene(gene, elementGene)
            else if (res)
                res = false
        }

        return res
    }

    protected fun updateGeneWithParameterValue(gene: ObjectGene, paramName: String, paramValue: String): Boolean {
        /*
            TODO: Support objects in query/header/path/cookie parameters. Unlike for
             the ArrayGene, here we don't support any kind of object in parameters
             other than body ones.

            TODO: Support XML?
         */

        var res = true

        try {
            val fields = Gson().fromJson(paramValue, Map::class.java)
            gene.fields.forEach {
                if (!updateGenesRecursivelyWithParameterValue(it, it.name, fields[it.name] as String?) && res)
                    res = false
            }
        } catch (ex: JsonSyntaxException) {
            res = logWarningAndReturnFalse(WARN_WRONG_JSON, paramName)
        }

        return res
    }

    protected fun updateGeneWithParameterValue(gene: MapGene<*>, paramName: String, paramValue: String): Boolean {
        /*
            TODO: Same comment as in ObjectGene
         */

        var res = true

        gene.elements.clear()

        try {
            val elements = Gson().fromJson(paramValue, Map::class.java)
            if (elements.size > MapGene.MAX_SIZE)
                gene.maxSize = elements.size
            elements.forEach { (key, value) ->
                val elementGene = gene.template.copy()
                elementGene.name = key as String
                elementGene.parent = gene
                if (updateGenesRecursivelyWithParameterValue(elementGene, elementGene.name, value as String?))
                    addGeneToMapGene(gene, elementGene)
                else if (res)
                    res = false
            }
        } catch (ex: JsonSyntaxException) {
            res = logWarningAndReturnFalse(WARN_WRONG_JSON, paramName)
        }

        return res
    }

    private fun addGeneToArrayGene(gene: ArrayGene<*>, elementGene: Gene) {
        when (elementGene) {
            is StringGene -> (gene as ArrayGene<StringGene>).elements.add(elementGene)
            is BooleanGene -> (gene as ArrayGene<BooleanGene>).elements.add(elementGene)
            is DoubleGene -> (gene as ArrayGene<DoubleGene>).elements.add(elementGene)
            is FloatGene -> (gene as ArrayGene<FloatGene>).elements.add(elementGene)
            is IntegerGene -> (gene as ArrayGene<IntegerGene>).elements.add(elementGene)
            is LongGene -> (gene as ArrayGene<LongGene>).elements.add(elementGene)
            is Base64StringGene -> (gene as ArrayGene<Base64StringGene>).elements.add(elementGene)
            is EnumGene<*> -> (gene as ArrayGene<EnumGene<*>>).elements.add(elementGene)
            is DateGene -> (gene as ArrayGene<DateGene>).elements.add(elementGene)
            is TimeGene -> (gene as ArrayGene<TimeGene>).elements.add(elementGene)
            is DateTimeGene -> (gene as ArrayGene<DateTimeGene>).elements.add(elementGene)
            is OptionalGene -> (gene as ArrayGene<OptionalGene>).elements.add(elementGene)
            is DisruptiveGene<*> -> (gene as ArrayGene<DisruptiveGene<*>>).elements.add(elementGene)
            is ArrayGene<*> -> (gene as ArrayGene<ArrayGene<*>>).elements.add(elementGene)
            is ObjectGene -> (gene as ArrayGene<ObjectGene>).elements.add(elementGene)
            is MapGene<*> -> (gene as ArrayGene<MapGene<*>>).elements.add(elementGene)
        }
    }

    private fun addGeneToMapGene(gene: MapGene<*>, elementGene: Gene) {
        when(elementGene) {
            is StringGene -> (gene as MapGene<StringGene>).elements.add(elementGene)
            is BooleanGene -> (gene as MapGene<BooleanGene>).elements.add(elementGene)
            is DoubleGene -> (gene as MapGene<DoubleGene>).elements.add(elementGene)
            is FloatGene -> (gene as MapGene<FloatGene>).elements.add(elementGene)
            is IntegerGene -> (gene as MapGene<IntegerGene>).elements.add(elementGene)
            is LongGene -> (gene as MapGene<LongGene>).elements.add(elementGene)
            is Base64StringGene -> (gene as MapGene<Base64StringGene>).elements.add(elementGene)
            is EnumGene<*> -> (gene as MapGene<EnumGene<*>>).elements.add(elementGene)
            is DateGene -> (gene as MapGene<DateGene>).elements.add(elementGene)
            is TimeGene -> (gene as MapGene<TimeGene>).elements.add(elementGene)
            is DateTimeGene -> (gene as MapGene<DateTimeGene>).elements.add(elementGene)
            is OptionalGene -> (gene as MapGene<OptionalGene>).elements.add(elementGene)
            is DisruptiveGene<*> -> (gene as MapGene<DisruptiveGene<*>>).elements.add(elementGene)
            is ArrayGene<*> -> (gene as MapGene<ArrayGene<*>>).elements.add(elementGene)
            is ObjectGene -> (gene as MapGene<ObjectGene>).elements.add(elementGene)
            is MapGene<*> -> (gene as MapGene<MapGene<*>>).elements.add(elementGene)
        }
    }

    private fun logWarningAndReturnFalse(message: String, vararg logArgs: String): Boolean {
        when (logArgs.size) {
            1 -> log.warn(message, logArgs[0])
            2 -> log.warn(message, logArgs[0], logArgs[1])
            3 -> log.warn(message, logArgs[0], logArgs[1], logArgs[2])
            4 -> log.warn(message, logArgs[0], logArgs[1], logArgs[2], logArgs[3])
        }
        return false
    }

}