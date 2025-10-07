package org.evomaster.core.output.dto

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
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
import org.evomaster.core.search.gene.wrapper.ChoiceGene
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

    fun write(testSuitePath: Path, testSuitePackage: String, outputFormat: OutputFormat, actionDefinitions: List<Action>) {
        calculateDtos(actionDefinitions)
        dtoCollector.forEach {
            when {
                outputFormat.isJava() -> JavaDtoOutput().writeClass(testSuitePath, testSuitePackage, outputFormat, it.value)
                else -> throw IllegalStateException("$outputFormat output format does not support DTOs as request payloads.")
            }
        }
    }

    private fun calculateDtos(actionDefinitions: List<Action>) {
        actionDefinitions.forEach { action ->
            action.getViewOfChildren().find { it is BodyParam }
            .let {
                val primaryGene = (it as BodyParam).primaryGene()
                val choiceGene = primaryGene.getWrappedGene(ChoiceGene::class.java)
                if (choiceGene != null) {
                    calculateDtoFromChoice(choiceGene, action.getName())
                } else {
                    calculateDtoFromNonChoiceGene(primaryGene.getLeafGene(), action.getName())
                }
            }
        }
    }

    private fun calculateDtoFromChoice(gene: ChoiceGene<*>, actionName: String) {
        // TODO: should we handle EnumGene?
        if (hasObjectOrArrayGene(gene)) {
            val dtoName = TestWriterUtils.safeVariableName(actionName)
            val dtoClass = DtoClass(dtoName)
            val children = gene.getViewOfChildren()
            // merge options into a single DTO
            children.forEach { childGene ->
                when (childGene) {
                    is ObjectGene -> populateDtoClass(dtoClass, childGene)
                    is ArrayGene<*> -> {
                        val template = childGene.template
                        if (template is ObjectGene) {
                            populateDtoClass(dtoClass, template)
                        }
                    }
                }
            }
            dtoCollector.put(dtoName, dtoClass)
        }
    }

    private fun hasObjectOrArrayGene(gene: ChoiceGene<*>): Boolean {
        return gene.getViewOfChildren().any { it is ObjectGene || it is ArrayGene<*> }
    }

    private fun calculateDtoFromNonChoiceGene(gene: Gene, actionName: String) {
        when {
            gene is ObjectGene -> calculateDtoFromObject(gene, actionName)
            gene is ArrayGene<*> -> calculateDtoFromArray(gene, actionName)
            isPrimitiveGene(gene) -> return
            else -> {
                throw IllegalStateException("Gene $gene is not supported for DTO payloads for action: $actionName")
            }
        }
    }

    private fun isPrimitiveGene(gene: Gene): Boolean {
        return when (gene) {
            is StringGene, is IntegerGene, is LongGene, is DoubleGene, is FloatGene, is BooleanGene -> true
            else -> false
        }
    }

    private fun calculateDtoFromObject(gene: ObjectGene, actionName: String) {
        // TODO: Determine strategy for objects that are not defined as a component and do not have a name
        val dtoName = gene.refType?:TestWriterUtils.safeVariableName(actionName)
        val dtoClass = DtoClass(dtoName)
        // TODO: add support for additionalFields
        populateDtoClass(dtoClass, gene)
        dtoCollector.put(dtoName, dtoClass)
    }

    private fun calculateDtoFromArray(gene: ArrayGene<*>, actionName: String) {
        val template = gene.template
        // TODO consider ChoiceGene. Primitive types won't be considered, an array of strings should not be wrapped
        //  into a DTO but just use List<String> for setting the payload.
        if (template is ObjectGene) {
            calculateDtoFromObject(template, actionName)
        } else {
            LoggingUtil.uniqueWarn(log, "Arrays of non custom objects are not collected as DTOs. Attempted at $actionName")
        }
    }

    private fun populateDtoClass(dtoClass: DtoClass, gene: ObjectGene) {
        gene.fixedFields.forEach { field ->
            try {
                val wrappedGene = field.getLeafGene()
                val dtoField = getDtoField(field.name, wrappedGene)
                dtoClass.addField(dtoField)
                if (wrappedGene is ObjectGene && !dtoCollector.contains(dtoField.type)) {
                    calculateDtoFromObject(wrappedGene, dtoField.type)
                }
                if (wrappedGene is ArrayGene<*> && wrappedGene.template is ObjectGene) {
                    calculateDtoFromObject(wrappedGene.template, StringUtils.capitalization(wrappedGene.name))
                }
            } catch (ex: Exception) {
                log.warn("A failure has occurred when collecting DTOs. \n"
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
                )
                assert(false)
            }
        }
    }

    private fun getDtoField(fieldName: String, field: Gene?): DtoField {
        return DtoField(fieldName, getDtoType(fieldName, field))
    }

    private fun getDtoType(fieldName: String, field: Gene?): String {
        return when (field) {
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
            is ArrayGene<*> -> "List<${getDtoType(field.name, field.template)}>"
            else -> throw Exception("Not supported gene at the moment: ${field?.javaClass?.simpleName} for field $fieldName")
        }
    }

    /**
     * @return collected DTOs by the DTO writer. Only for testing, otherwise we're breaking encapsulation.
     */
    @VisibleForTesting
    internal fun getCollectedDtos() : Map<String, DtoClass> {
        return dtoCollector
    }
}
