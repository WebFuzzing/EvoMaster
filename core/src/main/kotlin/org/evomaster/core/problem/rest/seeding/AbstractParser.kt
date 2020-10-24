package org.evomaster.core.problem.rest.seeding

import com.google.inject.Inject
import org.apache.commons.codec.binary.Base64
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.seeding.postman.PostmanParser
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractParser(
        protected val restSampler: RestSampler
) : Parser {

    @Inject
    protected lateinit var config: EMConfig

    @Inject
    protected lateinit var time : SearchTimeController

    protected val swagger = restSampler.getOpenAPI()

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PostmanParser::class.java)
    }

    fun updateGenesRecursivelyWithParameterValue(parameter: Param, paramValue: String?) {
        val rootGene = parameter.gene

        if (paramValue == null) {
            if (rootGene is OptionalGene)
                rootGene.isActive = false
            else
                log.warn("Required parameter {} was not found in a seeded request. Ignoring and keeping the parameter...", parameter.name)
            return
        }

        when (rootGene) {

            // Basic data type genes

            is StringGene -> rootGene.value = paramValue

            is BooleanGene -> {
                when (paramValue) {
                    "true" -> rootGene.value = true
                    "false" -> rootGene.value = false
                    else -> logBadParamAssignment("boolean", parameter.name, paramValue)
                }
            }

            is DoubleGene -> try { rootGene.value = paramValue.toDouble() }
                catch (e: NumberFormatException) {
                    logBadParamAssignment("double", parameter.name, paramValue)
                }

            is FloatGene -> try { rootGene.value = paramValue.toFloat() }
                catch (e: NumberFormatException) {
                    logBadParamAssignment("float", parameter.name, paramValue)
                }

            is IntegerGene -> try { rootGene.value = paramValue.toInt() }
                catch (e: NumberFormatException) {
                    logBadParamAssignment("integer", parameter.name, paramValue)
                }

            is LongGene -> try { rootGene.value = paramValue.toLong() }
                catch (e: NumberFormatException) {
                    logBadParamAssignment("long", parameter.name, paramValue)
                }


            // Non-basic data type but terminal genes

            is Base64StringGene -> {
                if (Base64.isBase64(paramValue))
                    rootGene.data.value = paramValue
                else
                    logBadParamAssignment("base64", parameter.name, paramValue)
            }

            is EnumGene<*> -> {
                if (rootGene.values.map { it.toString() }.contains(paramValue))
                    rootGene.index = rootGene.values.map { it.toString() }.indexOf(paramValue)
                else
                    logBadParamAssignment("enum", parameter.name, paramValue)
            }

            is DateGene -> updateGeneWithParameterValue(rootGene, parameter.name, paramValue)

            is TimeGene -> updateGeneWithParameterValue(rootGene, parameter.name, paramValue)

            is DateTimeGene -> {
                /*
                    Same comment as in Date and Time genes
                 */
                val dateTimeRegex = Regex("^\\d{4}-\\d{2}-\\d{2}([ T])\\d{2}:\\d{2}:\\d{2}(.\\d{3}Z)?$")
                if (paramValue.matches(dateTimeRegex)) {
                    updateGeneWithParameterValue(rootGene.date, parameter.name, paramValue)
                    updateGeneWithParameterValue(rootGene.time, parameter.name, paramValue)
                } else
                    logBadParamAssignment("date", parameter.name, paramValue)
            }

            else -> {
                TODO("Handling of gene " + rootGene.javaClass + " is not yet implemented")
            }
        }
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
            Same comment as in DateGene
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

    private fun logBadParamAssignment(paramType: String, paramName: String, paramValue: String) {
        log.warn("Attempt to set {} parameter {} with non-{} value {}. Ignoring and keeping the parameter the same...", paramType, paramName, paramType, paramValue)
    }

}