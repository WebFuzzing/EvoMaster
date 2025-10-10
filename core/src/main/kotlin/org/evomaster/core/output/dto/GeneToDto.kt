package org.evomaster.core.output.dto

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
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
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.utils.StringUtils

/**
 * Provides a mapping between a Gene and its DTO representation at use. Takes in the [OutputFormat] to delegate
 * writing of statements to the corresponding [DtoOutput]
 */
class GeneToDto(
    val outputFormat: OutputFormat
) {

    private var dtoOutput: DtoOutput = if (outputFormat.isJava()) {
        JavaDtoOutput()
    } else if (outputFormat.isKotlin()){
        KotlinDtoOutput()
    } else {
        throw IllegalStateException("$outputFormat output format does not support DTOs as request payloads.")
    }

    /**
     * @param leafGene to obtain the refType if the component is defined with a name
     * @param fallback to provide a fallback on the DTO named with the action if the component is defined inline
     *
     * @return the DTO name that will be used to instantiate the first variable
     */
    fun getDtoName(leafGene: Gene, fallback: String): String {
        return when (leafGene) {
            is ObjectGene -> leafGene.refType?:StringUtils.capitalization(TestWriterUtils.safeVariableName(fallback))
            is ArrayGene<*> -> {
                val template = leafGene.template
                if (template is ObjectGene) {
                    template.refType?:StringUtils.capitalization(TestWriterUtils.safeVariableName(fallback))
                } else {
                    // TODO handle arrays of basic data types
                    return getListType(fallback, template)
                }
            }
            else -> throw IllegalStateException("Gene $leafGene is not supported for DTO payloads for action: $fallback")
        }
    }

    /**
     * @param gene from which to extract the setter calls
     * @param dtoName that will be instantiated for payload
     * @param counter to provide uniqueness under the same DTO being used in a single test case
     *
     * @return a [DtoCall] object that can be written to the test case
     */
    fun getDtoCall(gene: Gene, dtoName: String, counter: Int): DtoCall {
        return when(gene) {
            is ObjectGene -> getObjectDtoCall(gene, dtoName, counter)
            is ArrayGene<*> -> getArrayDtoCall(gene, dtoName, counter, null)
            else -> throw RuntimeException("BUG: Gene $gene (with type ${this::class.java.simpleName}) should not be creating DTOs")
        }
    }

    private fun getObjectDtoCall(gene: ObjectGene, dtoName: String, counter: Int): DtoCall {
        val dtoVarName = "dto_${dtoName}_${counter}"

        val result = mutableListOf<String>()
        result.add(dtoOutput.getNewObjectStatement(dtoName, dtoVarName))

        val includedFields = gene.fields.filter {
            it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene))
        } .filter { it.isPrintable() }

        includedFields.forEach {
            val leafGene = it.getLeafGene()
            val attributeName = it.name
            when (leafGene) {
                is ObjectGene -> {
                    val childDtoCall = getDtoCall(leafGene, getDtoName(leafGene, attributeName), counter)

                    result.addAll(childDtoCall.objectCalls)
                    result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, childDtoCall.varName))
                }
                is ArrayGene<*> -> {
                    val childDtoCall = getArrayDtoCall(leafGene, getDtoName(leafGene, attributeName), counter, attributeName)

                    result.addAll(childDtoCall.objectCalls)
                    result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, childDtoCall.varName))
                }
                else -> {
                    result.add(dtoOutput.getSetterStatement(dtoVarName, attributeName, "${leafGene.getValueAsPrintableString(targetFormat = null)}${getValueSuffix(leafGene)}"))
                }
            }
        }

        return DtoCall(dtoVarName, result)
    }

    private fun getArrayDtoCall(gene: ArrayGene<*>, dtoName: String, counter: Int, targetAttribute: String?): DtoCall {
        val result = mutableListOf<String>()
        val template = gene.template

        val listType = getListType(dtoName,template)
        val listVarName = "list_${targetAttribute?:dtoName}_${counter}"
        result.add(dtoOutput.getNewListStatement(listType, listVarName))

        if (template is ObjectGene) {
            val childDtoName = template.refType?:StringUtils.capitalization(gene.name)
            var listCounter = 1
            gene.getViewOfElements().forEach {
                val childDtoCall = getDtoCall(it,childDtoName, listCounter++)
                result.addAll(childDtoCall.objectCalls)
                result.add(dtoOutput.getAddElementToListStatement(listVarName, childDtoCall.varName))
            }
        } else {
            gene.getViewOfElements().forEach {
                val leafGene = it.getLeafGene()
                result.add(dtoOutput.getAddElementToListStatement(listVarName, "${leafGene.getValueAsPrintableString(targetFormat = null)}${getValueSuffix(leafGene)}"))
            }
        }

        return DtoCall(listVarName, result)
    }

    private fun getListType(fieldName: String, gene: Gene): String {
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
            is ObjectGene -> gene.refType?:StringUtils.capitalization(fieldName)
            is ArrayGene<*> -> "List<${getListType(gene.name, gene.template)}>"
            else -> throw Exception("Not supported gene at the moment: ${gene?.javaClass?.simpleName} for field $fieldName")
        }
    }

    // According to documentation, a trailing constant is only needed for Long, Hexadecimal and Float
    // https://kotlinlang.org/docs/numbers.html#literal-constants-for-numbers
    private fun getValueSuffix(gene: Gene): String {
        return when (gene) {
            is LongGene -> "L"
            is FloatGene -> "f"
            else -> ""
        }
    }
}
