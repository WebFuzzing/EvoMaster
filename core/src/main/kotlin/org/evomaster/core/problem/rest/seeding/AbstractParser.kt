package org.evomaster.core.problem.rest.seeding

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.swagger.v3.oas.models.OpenAPI
import org.apache.commons.codec.binary.Base64
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.seeding.postman.PostmanParser
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        private val log: Logger = LoggerFactory.getLogger(PostmanParser::class.java)
    }

    fun updateGenesRecursivelyWithParameterValue(gene: Gene, paramName: String, paramValue: String?) {
        // Optional parameter not present in request
        if (paramValue == null) {
            if (gene is OptionalGene)
                gene.isActive = false
            else
                log.warn("Required parameter {} was not found in a seeded request. Ignoring and keeping the parameter...", paramName)
            return
        }

        when (gene) {

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

    protected fun updateGeneWithParameterValue(gene: StringGene, paramName: String, paramValue: String) {
        gene.value = paramValue
    }

    protected fun updateGeneWithParameterValue(gene: BooleanGene, paramName: String, paramValue: String) {
        when (paramValue) {
            "true" -> gene.value = true
            "false" -> gene.value = false
            else -> logBadParamAssignment("boolean", paramName, paramValue)
        }
    }

    protected fun updateGeneWithParameterValue(gene: DoubleGene, paramName: String, paramValue: String) {
        try {
            gene.value = paramValue.toDouble()
        } catch (e: NumberFormatException) {
            logBadParamAssignment("double", paramName, paramValue)
        }
    }

    protected fun updateGeneWithParameterValue(gene: FloatGene, paramName: String, paramValue: String) {
        try {
            gene.value = paramValue.toFloat()
        } catch (e: NumberFormatException) {
            logBadParamAssignment("float", paramName, paramValue)
        }
    }

    protected fun updateGeneWithParameterValue(gene: IntegerGene, paramName: String, paramValue: String) {
        try {
            gene.value = paramValue.toInt()
        } catch (e: NumberFormatException) {
            logBadParamAssignment("integer", paramName, paramValue)
        }
    }

    protected fun updateGeneWithParameterValue(gene: LongGene, paramName: String, paramValue: String) {
        try {
            gene.value = paramValue.toLong()
        } catch (e: NumberFormatException) {
            logBadParamAssignment("long", paramName, paramValue)
        }
    }

    protected fun updateGeneWithParameterValue(gene: Base64StringGene, paramName: String, paramValue: String) {
        if (Base64.isBase64(paramValue))
            gene.data.value = paramValue
        else
            logBadParamAssignment("base64", paramName, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: EnumGene<*>, paramName: String, paramValue: String) {
        if (gene.values.map { it.toString() }.contains(paramValue))
            gene.index = gene.values.map { it.toString() }.indexOf(paramValue)
        else
            logBadParamAssignment("enum", paramName, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: DateGene, paramName: String, paramValue: String) {
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
                log.warn("Attempt to set date parameter {} with invalid date {}. Ignoring and keeping the parameter the same...", paramName, paramValue)
        } else
            logBadParamAssignment("date", paramName, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: TimeGene, paramName: String, paramValue: String) {
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
                log.warn("Attempt to set time parameter {} with invalid time {}. Ignoring and keeping the parameter the same...", paramName, paramValue)
        } else
            logBadParamAssignment("time", paramName, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: DateTimeGene, paramName: String, paramValue: String) {
        /*
            TODO: Same comment as in Date and Time genes
         */
        val dateTimeRegex = Regex("^\\d{4}-\\d{2}-\\d{2}([ T])\\d{2}:\\d{2}:\\d{2}(.\\d{3}Z)?$")
        if (paramValue.matches(dateTimeRegex)) {
            updateGeneWithParameterValue(gene.date, paramName, paramValue)
            updateGeneWithParameterValue(gene.time, paramName, paramValue)
        } else
            logBadParamAssignment("date-time", paramName, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: OptionalGene, paramName: String, paramValue: String) {
        updateGenesRecursivelyWithParameterValue(gene.gene, gene.name, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: DisruptiveGene<*>, paramName: String, paramValue: String) {
        updateGenesRecursivelyWithParameterValue(gene.gene, gene.gene.name, paramValue)
    }

    protected fun updateGeneWithParameterValue(gene: ArrayGene<*>, paramName: String, paramValue: String) {
        /*
            Here we may have an array as a query/header/path/cookie parameter
            or inside a request body. In the first case, we assume the values
            are comma-separated.

            TODO: Support more serialization options. This involves not only different
             separators other than ',' (e.g., '|' and ' '), but also different parsing
             of the request. For example, in query parameters it is possible to have
             arrays expressed as "/endpoint?param=a&param=b&param=c", which is equal
             to "/endpoint?param=a,b,c". However, for this, it would be necessary to
             look for the serialization style and explode properties in the Swagger.
             Reference: https://swagger.io/docs/specification/serialization/

            TODO: Support nested objects and arrays in non-body parameters
         */

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
            updateGenesRecursivelyWithParameterValue(elementGene, elementGene.name, element as String?)
            addGeneToArrayGene(gene, elementGene)
        }
    }

    protected fun updateGeneWithParameterValue(gene: ObjectGene, paramName: String, paramValue: String) {
        /*
            TODO: Support objects in query/header/path/cookie parameters. Unlike for
             the ArrayGene, here we don't support any kind of object in parameters
             other than body ones.

            TODO: Support XML?
         */

        try {
            val fields = Gson().fromJson(paramValue, Map::class.java)
            gene.fields.forEach { updateGenesRecursivelyWithParameterValue(it, it.name, fields[it.name] as String?) }
        } catch (ex: JsonSyntaxException) {
            log.warn("Failed to parse parameter {} as JSON", paramName)
        }
    }

    protected fun updateGeneWithParameterValue(gene: MapGene<*>, paramName: String, paramValue: String) {
        /*
            TODO: Same comment as in ObjectGene
         */

        gene.elements.clear()

        try {
            val elements = Gson().fromJson(paramValue, Map::class.java)
            if (elements.size > MapGene.MAX_SIZE)
                gene.maxSize = elements.size
            elements.forEach { (key, value) ->
                val elementGene = gene.template.copy()
                elementGene.name = key as String
                elementGene.parent = gene
                updateGenesRecursivelyWithParameterValue(elementGene, elementGene.name, value as String?)
                addGeneToMapGene(gene, elementGene)
            }
        } catch (ex: JsonSyntaxException) {
            log.warn("Failed to parse parameter {} as JSON", paramName)
        }
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

    private fun logBadParamAssignment(paramType: String, paramName: String, paramValue: String) {
        log.warn("Attempt to set {} parameter {} with non-{} value {}. Ignoring and keeping the parameter the same...", paramType, paramName, paramType, paramValue)
    }

}