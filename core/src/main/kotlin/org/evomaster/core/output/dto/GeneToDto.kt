package org.evomaster.core.output.dto

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
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

/**
 * Provides a mapping between a Gene and its DTO representation at use. Takes in the [OutputFormat] to delegate
 * writing of statements to the corresponding [DtoOutput]
 */
class GeneToDto(
    val outputFormat: OutputFormat
) {

    private val log: Logger = LoggerFactory.getLogger(GeneToDto::class.java)

    private var dtoOutput: DtoOutput = if (outputFormat.isJava()) {
        JavaDtoOutput()
    } else if (outputFormat.isKotlin()){
        KotlinDtoOutput()
    } else {
        throw IllegalStateException("$outputFormat output format does not support DTOs as request payloads.")
    }

    /**
     * @param gene to obtain the refType if the component is defined with a name
     * @param fallback to provide a fallback on the DTO named with the action if the component is defined inline
     * @param capitalize to determine if the DTO string name must be capitalized for test case writing
     *
     * @return the DTO name that will be used to instantiate the first variable
     */
    fun getDtoName(gene: Gene, fallback: String, capitalize: Boolean): String {
        return when (gene) {
            is ObjectGene -> TestWriterUtils.safeVariableName(gene.refType?:fallback)
            is ArrayGene<*> -> {
                val template = gene.template
                if (template is ObjectGene) {
                    TestWriterUtils.safeVariableName(template.refType?:fallback)
                } else {
                    // TODO handle arrays of basic data types
                    getListType(fallback, template, capitalize)
                }
            }
            is ChoiceGene<*> -> TestWriterUtils.safeVariableName(fallback)
            is FixedMapGene<*,*> -> TestWriterUtils.safeVariableName(fallback)
            else -> throw IllegalStateException("Gene $gene is not supported for DTO payloads for action: $fallback")
        }
    }

    /**
     * @param gene from which to extract the setter calls
     * @param dtoName that will be instantiated for payload
     * @param counters list to provide uniqueness under the same DTO being used in a single test case
     * @param capitalize to determine if the DTO string name must be capitalized for test case writing
     *
     * @return a [DtoCall] object that can be written to the test case
     */
    fun getDtoCall(gene: Gene, dtoName: String, counters: MutableList<Int>, capitalize: Boolean): DtoCall {
        return when(gene) {
            is ObjectGene -> getObjectDtoCall(gene, dtoName, counters)
            is ArrayGene<*> -> getArrayDtoCall(gene, dtoName, counters, null, capitalize)
            is ChoiceGene<*> -> getDtoCall(gene.activeGene(), dtoName, counters, capitalize)
            is FixedMapGene<*,*> -> getFixedMapGeneDtoCall(gene, dtoName, counters)
            else -> throw RuntimeException("BUG: Gene $gene (with type ${this::class.java.simpleName}) should not be creating DTOs")
        }
    }

    private fun getObjectDtoCall(gene: ObjectGene, dtoName: String, counters: MutableList<Int>): DtoCall {
        val dtoVarName = "dto_${dtoName}_${counters.joinToString("_")}"

        val result = mutableListOf<String>()
        result.add(dtoOutput.getNewObjectStatement(dtoName, dtoVarName))

        val includedFields = gene.fixedFields.filter {
            it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene))
        } .filter { it.isPrintable() }

        includedFields.forEach {
            val leafGene = it.getLeafGene()
            val attributeName = it.name
            when (leafGene) {
                is ObjectGene -> {
                    val childDtoCall = getDtoCall(leafGene, getDtoName(leafGene, attributeName, true), counters, true)

                    result.addAll(childDtoCall.objectCalls)
                    result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, childDtoCall.varName))
                }
                is ArrayGene<*> -> {
                    val childDtoCall = getArrayDtoCall(leafGene, getDtoName(leafGene, attributeName, true), counters, attributeName, true)

                    result.addAll(childDtoCall.objectCalls)
                    result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, childDtoCall.varName))
                }
                else -> {
                    val parent = leafGene.parent
                    if (leafGene is EnumGene<*> && parent is ChoiceGene<*>) {
                        val children = parent.getViewOfChildren()
                        val otherChoice = children.find { child -> child != leafGene }
                        result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, "${leafGene.getValueAsPrintableString(targetFormat = outputFormat)}${getValueSuffix(otherChoice)}"))
                    } else {
                        result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, "${leafGene.getValueAsPrintableString(targetFormat = outputFormat)}${getValueSuffix(leafGene)}"))
                    }
                }
            }
        }

        if (!gene.isFixed) {
            val additionalProperties = gene.additionalFields!!.filter {
                it.isPrintable() && !isInactiveOptionalGene(it)
            } as List<PairGene<Gene, Gene>>
            setAdditionalProperties(dtoName, dtoVarName, result, counters, additionalProperties)
        }

        return DtoCall(dtoVarName, result)
    }

    private fun getFixedMapGeneDtoCall(gene: FixedMapGene<*, *>, dtoName: String, counters: MutableList<Int>): DtoCall {
        val dtoVarName = "dto_${dtoName}_${counters.joinToString("_")}"
        val result = mutableListOf<String>()
        result.add(dtoOutput.getNewObjectStatement(dtoName, dtoVarName))
        val additionalProperties = gene.getViewOfChildren()!!.filter {
            it.isPrintable() && !isInactiveOptionalGene(it)
        } as List<PairGene<Gene, Gene>>
        setAdditionalProperties(dtoName, dtoVarName, result, counters, additionalProperties)
        return DtoCall(dtoVarName, result)
    }

    private fun setAdditionalProperties(dtoName: String, dtoVarName: String, result: MutableList<String>, counters: MutableList<Int>, additionalProperties: List<PairGene<Gene, Gene>>) {
        if (additionalProperties.isNotEmpty()) {
            var additionalPropertiesCounter = 1
            additionalProperties.forEach { field ->
                try {
                    val childCounter = mutableListOf<Int>()
                    childCounter.addAll(counters)
                    childCounter.add(additionalPropertiesCounter++)
                    val key = (field as PairGene<StringGene, Gene>).first.getLeafGene()
                        .getValueAsPrintableString(targetFormat = outputFormat)
                    val leafGene = (field as PairGene<StringGene, Gene>).second.getLeafGene()
                    val additionalPropertiesVarName = when (leafGene) {
                        is ObjectGene -> {
                            val attributeName = leafGene.refType ?: "${dtoName}_ap"
                            val childDtoCall =
                                getDtoCall(leafGene, getDtoName(leafGene, attributeName, true), childCounter, true)
                            result.addAll(childDtoCall.objectCalls)
                            childDtoCall.varName
                        }

                        is ArrayGene<*> -> {
                            val attributeName = if (leafGene.template is ObjectGene) {
                                leafGene.template.refType ?: "${dtoName}_ap"
                            } else {
                                getListType("", leafGene.template, true)
                            }
                            val childDtoCall = getArrayDtoCall(
                                leafGene,
                                getDtoName(leafGene, attributeName, true),
                                childCounter,
                                attributeName,
                                true
                            )

                            result.addAll(childDtoCall.objectCalls)
                            childDtoCall.varName
                        }

                        else -> throw IllegalStateException("Additional properties should only be Map related genes")
                    }
                    result.add(
                        dtoOutput.getAddElementToAdditionalPropertiesStatement(
                            dtoVarName,
                            key,
                            additionalPropertiesVarName
                        )
                    )
                } catch (ex: Exception) {
                    log.warn(
                        "A failure has occurred when writing DTOs. \n"
                                + "Exception: ${ex.localizedMessage} \n"
                                + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
                    )
                    assert(false)
                }
            }
        }
    }

    private fun getArrayDtoCall(gene: ArrayGene<*>, dtoName: String, counters: MutableList<Int>, targetAttribute: String?, capitalize: Boolean): DtoCall {
        val result = mutableListOf<String>()
        val template = gene.template

        val listType = getListType(dtoName,template, capitalize)
        val listVarName = "list_${targetAttribute?:dtoName}_${counters.joinToString("_")}"
        result.add(dtoOutput.getNewListStatement(listType, listVarName))

        if (template is ObjectGene) {
            val childDtoName = template.refType?: if (capitalize) StringUtils.capitalization(dtoName) else dtoName
            var listCounter = 1
            gene.getViewOfElements().forEach {
                val childCounter = mutableListOf<Int>()
                childCounter.addAll(counters)
                childCounter.add(listCounter++)
                val childDtoCall = getDtoCall(it,childDtoName, childCounter, true)
                result.addAll(childDtoCall.objectCalls)
                result.add(dtoOutput.getAddElementToListStatement(listVarName, childDtoCall.varName))
            }
        } else {
            gene.getViewOfElements().forEach {
                val leafGene = it.getLeafGene()
                result.add(dtoOutput.getAddElementToListStatement(listVarName, "${leafGene.getValueAsPrintableString(targetFormat = outputFormat)}${getValueSuffix(leafGene)}"))
            }
        }

        return DtoCall(listVarName, result)
    }

    private fun getListType(fieldName: String, gene: Gene, capitalize: Boolean): String {
        return when (gene) {
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
            is ObjectGene -> gene.refType?: if (capitalize) StringUtils.capitalization(fieldName) else fieldName
            is ArrayGene<*> -> if (outputFormat.isJava()) "List<${getListType(gene.name, gene.template, capitalize)}>" else "MutableList<${getListType(gene.name, gene.template, capitalize)}>"
            else -> throw Exception("Not supported gene at the moment: ${gene?.javaClass?.simpleName} for field $fieldName")
        }
    }

    // According to documentation, a trailing constant is only needed for Long, Hexadecimal and Float
    // https://kotlinlang.org/docs/numbers.html#literal-constants-for-numbers
    private fun getValueSuffix(gene: Gene?): String {
        return when (gene) {
            is LongGene -> "L"
            is FloatGene -> "f"
            else -> ""
        }
    }
}
