package org.evomaster.core.output.dto

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.utils.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * When creating tests for REST APIs, enabling the [EMConfig.dtoForRequestPayload] feature causes EVoMaster to use
 * DTOs when handling request payloads instead of stringifying the JSON payload.
 *
 * Using DTOs provides a major advantage towards code readability and sustainability. It is more flexible and scales
 * better.
 */
class DtoWriter {

    private val log: Logger = LoggerFactory.getLogger(DtoWriter::class.java)

    /**
     * [MutableMap] that will collect the different DTOs to be handled by the test suite. Keys in the map are the
     * DTO name String which translates directly into the .kt or .java class name in filesystem.
     */
    private val dtoCollector: MutableMap<String, DtoClass> = mutableMapOf()

    fun write(testSuitePath: Path, outputFormat: OutputFormat, actionDefinitions: List<Action>) {
        getDtos(actionDefinitions)
        dtoCollector.forEach {
            when {
                outputFormat.isJava() -> JavaDtoWriter.write(testSuitePath, outputFormat, it.value)
                else -> throw IllegalStateException("$outputFormat output format does not support DTOs as request payloads.")
            }
        }
    }

    private fun getDtos(actionDefinitions: List<Action>) {
        actionDefinitions.forEach { action ->
            action.getViewOfChildren().find { it is BodyParam }
            .let {
                val primaryGene = GeneUtils.getWrappedValueGene((it as BodyParam).primaryGene())
                // TODO: Payloads could also be json arrays, analyze ArrayGene
                if (primaryGene is ObjectGene) {
                    getDtoFromObject(primaryGene, action.getName())
                }
            }
        }
    }

    private fun getDtoFromObject(gene: ObjectGene, actionName: String) {
        // TODO: Determine strategy for objects that are not defined as a component and do not have a name
        // TODO: consider an inline schema using more than one possible component: any/one/allOf[object,object]
        val dtoName = gene.refType?:TestWriterUtils.safeVariableName(actionName)
        val dtoClass = DtoClass(dtoName)
        // TODO: add support for additionalFields
        gene.fixedFields.forEach { field ->
            try {
                val wrappedGene = GeneUtils.getWrappedValueGene(field)
                val dtoField = getDtoField(field.name, wrappedGene)
                dtoClass.addField(dtoField)
                if (wrappedGene is ObjectGene && !dtoCollector.contains(dtoField.type)) {
                    getDtoFromObject(wrappedGene, dtoField.type)
                }
            } catch (ex: Exception) {
                log.warn("A failure has occurred when collecting DTOs. \n"
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
                )
                assert(false)
            }
        }
        dtoCollector.put(dtoName, dtoClass)
    }

    private fun getDtoField(fieldName: String, field: Gene?): DtoField {
        return DtoField(fieldName, when (field) {
            // TODO: handle nested arrays, objects and extend type system for dto fields
            is StringGene -> "String"
            is IntegerGene -> "Integer"
            is LongGene -> "Long"
            is DoubleGene -> "Double"
            is FloatGene -> "Float"
            is Base64StringGene -> "String"
            // Time, Date, DateTime and Regex genes will be handled with strings at the moment. In the future we'll evaluate if it's worth having any validation
            is DateGene -> "String"
            is TimeGene -> "String"
            is DateTimeGene -> "String"
            is RegexGene -> "String"
            is BooleanGene -> "Boolean"
            is ObjectGene -> field.refType?:StringUtils.capitalization(fieldName)
            else -> throw Exception("Not supported gene at the moment: ${field?.javaClass?.simpleName}")
        })
    }

    /**
     * @return collected DTOs by the DTO writer. Only for testing, otherwise we're breaking encapsulation.
     */
    @VisibleForTesting
    internal fun getCollectedDtos() : Map<String, DtoClass> {
        return dtoCollector
    }
}
