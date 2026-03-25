package org.evomaster.core.output.dto

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils.isInactiveOptionalGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
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
class DtoWriter(
    val outputFormat: OutputFormat
) {

    private val log: Logger = LoggerFactory.getLogger(DtoWriter::class.java)

    /**
     * [MutableMap] that will collect the different DTOs to be handled by the test suite. Keys in the map are the
     * DTO name String which translates directly into the .kt or .java class name in filesystem.
     */
    private val dtoCollector: MutableMap<String, DtoClass> = mutableMapOf()

    fun write(testSuitePath: Path, testSuitePackage: String, solution: Solution<*>) {
        calculateDtos(solution)
        val dtoOutput = when {
            outputFormat.isJava() -> JavaDtoOutput()
            outputFormat.isKotlin() -> KotlinDtoOutput()
            else -> throw IllegalStateException("$outputFormat output format does not support DTOs as request payloads.")
        }
        dtoCollector.forEach {
            dtoOutput.writeClass(outputFormat, testSuitePath, testSuitePackage, it.value)
        }
    }

    fun containsDtos(): Boolean {
        return dtoCollector.isNotEmpty()
    }

    private fun calculateDtos(solution: Solution<*>) {
        solution.individuals.forEach { evaluatedIndividual ->
            evaluatedIndividual.evaluatedMainActions().forEach { evaluatedAction ->
                val call = evaluatedAction.action as HttpWsAction
                val bodyParam = call.parameters.find { p -> p is BodyParam } as BodyParam?
                if (bodyParam != null) {
                    val primaryGene = bodyParam.primaryGene()
                    val choiceGene = primaryGene.getWrappedGene(ChoiceGene::class.java)
                    if (choiceGene != null) {
                        calculateDtoFromChoice(choiceGene, call.getName())
                    } else {
                        calculateDtoFromNonChoiceGene(primaryGene.getLeafGene(), call.getName())
                    }
                }
            }
        }
    }


    private fun calculateDtoFromChoice(gene: ChoiceGene<*>, actionName: String) {
        if (hasObjectOrArrayGene(gene)) {
            val dtoName = TestWriterUtils.safeVariableName(actionName)

            val dtoClass = dtoCollector.computeIfAbsent(dtoName) { DtoClass(dtoName) }
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
            dtoCollector[dtoName] = dtoClass
        }
    }

    private fun hasObjectOrArrayGene(gene: ChoiceGene<*>): Boolean {
        return gene.getViewOfChildren().any { it is ObjectGene || it is ArrayGene<*> }
    }

    private fun calculateDtoFromNonChoiceGene(gene: Gene, actionName: String) {
        when {
            gene is ObjectGene -> calculateDtoFromObject(gene, actionName)
            gene is ArrayGene<*> -> calculateDtoFromArray(gene, actionName)
            gene is FixedMapGene<*, *> -> calculateDtoFromFixedMapGene(gene, actionName)
            isPrimitiveGene(gene) -> return
            else -> {
                throw IllegalStateException("Gene $gene is not supported for DTO payloads for action: $actionName")
            }
        }
    }

    private fun calculateDtoFromFixedMapGene(gene: FixedMapGene<*, *>, actionName: String) {
        val dtoName = TestWriterUtils.safeVariableName(actionName)
        val dtoClass = dtoCollector.computeIfAbsent(dtoName) { DtoClass(dtoName) }
        val additionalProperties = gene.getViewOfChildren()!!.filter {
            it.isPrintable() && !isInactiveOptionalGene(it)
        } as List<PairGene<Gene, Gene>>
        addAdditionalProperties(dtoClass, additionalProperties)
        dtoCollector[dtoName] = dtoClass
    }

    private fun isPrimitiveGene(gene: Gene): Boolean {
        return when (gene) {
            is StringGene, is IntegerGene, is LongGene, is DoubleGene, is FloatGene, is BooleanGene -> true
            else -> false
        }
    }

    private fun calculateDtoFromObject(gene: ObjectGene, actionName: String) {
        // TODO: Determine strategy for objects that are not defined as a component and do not have a name
        val dtoName = TestWriterUtils.safeVariableName(gene.refType?:actionName)
        val dtoClass = dtoCollector.computeIfAbsent(dtoName) { DtoClass(dtoName) }
        populateDtoClass(dtoClass, gene)
        dtoCollector[dtoName] = dtoClass
    }

    private fun calculateDtoFromArray(gene: ArrayGene<*>, actionName: String) {
        val template = gene.template
        // Primitive types won't be considered, an array of strings should not be wrapped
        // into a DTO but just use List<String> for setting the payload.
        if (template is ObjectGene) {
            calculateDtoFromObject(template, actionName)
        } else {
            LoggingUtil.uniqueWarn(log, "Arrays of non custom objects are not collected as DTOs. Attempted at $actionName")
        }
    }

    private fun populateDtoClass(dtoClass: DtoClass, gene: ObjectGene) {
        val includedFields = gene.fixedFields.filter {
            it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene))
        } .filter { it.isPrintable() }

        includedFields.forEach { field ->
            try {
                val wrappedGene = field.getLeafGene()
                val dtoField = getDtoField(field.name, wrappedGene)
                dtoClass.addField(field.name, dtoField)
                if (wrappedGene is ObjectGene) {
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
        if (!gene.isFixed) {
            val additionalFields = gene.additionalFields!!.filter {
                it.isPrintable() && !isInactiveOptionalGene(it)
            } as List<PairGene<Gene, Gene>>
            addAdditionalProperties(dtoClass, additionalFields)
        }
    }

    private fun addAdditionalProperties(dtoClass: DtoClass, additionalProperties: List<PairGene<Gene, Gene>>) {
        if (additionalProperties.isEmpty()) {
            return
        }
        additionalProperties.forEach { field ->
            try {
                val wrappedGene = (field as PairGene<StringGene, Gene>).second.getLeafGene()
                if (wrappedGene is ObjectGene) {
                    val additionalPropertiesDtoName = wrappedGene.refType?:"${dtoClass.name}_ap"
                    dtoClass.additionalPropertiesDtoName = additionalPropertiesDtoName
                    calculateDtoFromObject(wrappedGene, additionalPropertiesDtoName)
                }
                if (wrappedGene is ArrayGene<*> && wrappedGene.template is ObjectGene) {
                    val additionalPropertiesDtoName = wrappedGene.template.refType?:"${dtoClass.name}_ap"
                    dtoClass.additionalPropertiesDtoName = additionalPropertiesDtoName
                    calculateDtoFromObject(wrappedGene.template, additionalPropertiesDtoName)
                }
            } catch (ex: Exception) {
                log.warn(
                    "A failure has occurred when collecting DTO additional properties. \n"
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
            is StringGene -> "String"
            is IntegerGene -> if (outputFormat.isJava()) "Integer" else "Int"
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
            is ArrayGene<*> -> if (outputFormat.isJava()) "List<${getDtoType(field.name, field.template)}>" else "MutableList<${getDtoType(field.name, field.template)}>"
            is EnumGene<*> -> field.getValueType(outputFormat.isKotlin())
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
