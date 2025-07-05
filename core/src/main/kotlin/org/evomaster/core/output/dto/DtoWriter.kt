package org.evomaster.core.output.dto

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.utils.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

object DtoWriter {

    private val log: Logger = LoggerFactory.getLogger(DtoWriter::class.java)
    private val dtoCollector: MutableMap<String, DtoClass> = mutableMapOf()

    fun write(testSuitePath: Path, outputFormat: OutputFormat, actionDefinitions: List<Action>) {
        getDtos(actionDefinitions)
        dtoCollector.forEach {
            JavaDtoWriter.write(testSuitePath, outputFormat, it.value)
        }
    }

    private fun getDtos(actionDefinitions: List<Action>) {
        actionDefinitions.forEach { action ->
            action.getViewOfChildren().forEach { child ->
                if (child is BodyParam) {
                    val primaryGene = GeneUtils.getWrappedValueGene(child.primaryGene())
                    // TODO: Payloads could also be json arrays, analyze ArrayGene
                    if (primaryGene is ObjectGene) {
                        getDtoFromObject(primaryGene, action.getName())
                    }
                }
            }
        }
    }

    private fun getDtoFromObject(gene: ObjectGene, actionName: String) {
        // TODO: Determine strategy for objects that are not defined as a component and do not have a name
        val dtoName = gene.refType?:TestWriterUtils.safeVariableName(actionName)
        val dtoClass = DtoClass(dtoName)
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
            // Time and Date genes will be handled with strings at the moment. In the future we'll evaluate if it's worth having any validation
            is DateGene -> "String"
            is TimeGene -> "String"
            is BooleanGene -> "Boolean"
            is ObjectGene -> StringUtils.capitalization(fieldName)
            else -> throw Exception("Not supported gene at the moment: ${field?.javaClass?.simpleName}")
        })
    }
}
