package org.evomaster.core.output

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.text.StringEscapeUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.database.dynamodb.DynamoDbValueType
import org.evomaster.core.search.action.EvaluatedDynamoDbAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene

/**
 * Generates DynamoDB initialization code for emitted tests.
 */
object DynamoDbWriter {

    private val ddbJsonMapper = ObjectMapper()

    /**
     * Generates DynamoDB insert actions for a test case.
     *
     * @param format format of the generated test
     * @param actions evaluated DynamoDB actions to generate
     * @param lines destination for the generated code
     * @param groupIndex suffix distinguishing this initialization group
     * @param dynamoDbInsertionVars previous DynamoDB insertion variable names and their result variable names
     * @param skipFailure whether failed insertions should be omitted
     */
    fun handleDynamoDbInitialization(
        format: OutputFormat,
        actions: List<EvaluatedDynamoDbAction>,
        lines: Lines,
        groupIndex: String = "",
        dynamoDbInsertionVars: MutableList<Pair<String, String>>,
        skipFailure: Boolean
    ) {
        val selected = actions.filter { !skipFailure || it.dynamoDbResult.getInsertExecutionResult() }
        if (selected.isEmpty()) return
        val insertionVar = "insertions_dynamodb${groupIndex}"
        val resultVar = "${insertionVar}_result"
        val previousVar = dynamoDbInsertionVars.joinToString(", ") { it.first }
        lines.add(if (format.isJava()) "List<DynamoDbInsertionDto> $insertionVar = dynamoDb($previousVar)" else "val $insertionVar = dynamoDb($previousVar)")
        lines.indent()
        selected.forEach { evaluated ->
            val action = evaluated.dynamoDbAction
            lines.add(".insertInto(\"${escape(action.tableName, format)}\")")
            action.attributes.forEach { attribute ->
                val name = "\"${escape(attribute.attributeName, format)}\""
                val value = when (attribute.type) {
                    DynamoDbValueType.MAP,
                    DynamoDbValueType.LIST,
                    DynamoDbValueType.STRING_SET,
                    DynamoDbValueType.NUMBER_SET,
                    DynamoDbValueType.NULL -> {
                        val ddbJson = ddbJsonMapper.writeValueAsString(toDynamoDbJsonNode(attribute.toDto()))
                        "\"${escape(ddbJson, format)}\""
                    }
                    DynamoDbValueType.STRING -> {
                        val raw = attribute.gene.getValueAsRawString()
                        val literal = "'${raw.replace("'", "''")}'"
                        "\"${escape(literal, format)}\""
                    }
                    DynamoDbValueType.NUMBER -> {
                        val raw = (attribute.gene as? BigDecimalGene)?.value?.toPlainString()
                            ?: attribute.gene.getValueAsRawString()
                        "\"${escape(raw, format)}\""
                    }
                    DynamoDbValueType.BOOLEAN -> {
                        val raw = (attribute.gene as? BooleanGene)?.value?.toString()
                            ?: attribute.gene.getValueAsRawString()
                        "\"$raw\""
                    }
                }
                val method = when (attribute.type) {
                    DynamoDbValueType.MAP,
                    DynamoDbValueType.LIST,
                    DynamoDbValueType.STRING_SET,
                    DynamoDbValueType.NUMBER_SET,
                    DynamoDbValueType.NULL -> "ddbJson"
                    DynamoDbValueType.STRING,
                    DynamoDbValueType.NUMBER,
                    DynamoDbValueType.BOOLEAN -> "d"
                }
                lines.add(".$method($name, $value)")
            }
        }
        lines.add(".dtos()")
        lines.appendSemicolon()
        lines.deindent()
        val declaration = if (format.isJava()) "DynamoDbInsertionResultsDto " else "val "
        lines.add("${declaration}${resultVar} = controller.execInsertionsIntoDynamoDb($insertionVar)")
        lines.appendSemicolon()
        dynamoDbInsertionVars.add(insertionVar to resultVar)
    }

    /**
     * Converts a typed attribute DTO tree into an AWS DynamoDB JSON wire map.
     */
    private fun toDynamoDbJsonNode(dto: DynamoDbAttributeValueDto): Map<String, Any?> = when (dto.type) {
        DynamoDbValueTypeDto.STRING -> mapOf(dto.type.wireCode to dto.value)
        DynamoDbValueTypeDto.NUMBER -> mapOf(dto.type.wireCode to dto.value)
        DynamoDbValueTypeDto.BOOLEAN -> mapOf(dto.type.wireCode to dto.value.toBoolean())
        DynamoDbValueTypeDto.NULL -> mapOf(dto.type.wireCode to true)
        DynamoDbValueTypeDto.MAP -> {
            val children = linkedMapOf<String, Any?>()
            dto.mapValue?.entries?.sortedBy { it.key }?.forEach { (key, child) ->
                children[key] = toDynamoDbJsonNode(child)
            }
            mapOf(dto.type.wireCode to children)
        }
        DynamoDbValueTypeDto.LIST -> {
            val children = dto.listValue?.map { toDynamoDbJsonNode(it) } ?: emptyList()
            mapOf(dto.type.wireCode to children)
        }
        DynamoDbValueTypeDto.STRING_SET,
        DynamoDbValueTypeDto.NUMBER_SET -> mapOf(dto.type.wireCode to (dto.setValue ?: emptyList<String>()))
        null -> throw IllegalArgumentException("Unsupported DynamoDB attribute type: null")
    }

    /**
     * Escapes string literals for the target source code format.
     *
     * @param value raw string value to escape
     * @param format target output programming language format
     * @return escaped string representation
     */
    private fun escape(value: String, format: OutputFormat): String =
        StringEscapeUtils.escapeJava(value).let { if (format.isKotlin()) it.replace("$", "\\$") else it }
}
