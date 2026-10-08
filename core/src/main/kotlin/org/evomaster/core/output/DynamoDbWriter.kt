package org.evomaster.core.output

import org.apache.commons.text.StringEscapeUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.action.EvaluatedDynamoDbAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene

/** Generates DynamoDB initialization code for emitted tests. */
object DynamoDbWriter {

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
                    DynamoDbScalarTypeDto.STRING -> (attribute.gene as StringGene).getValueAsPrintableString(targetFormat = format)
                    DynamoDbScalarTypeDto.NUMBER -> "\"${(attribute.gene as BigDecimalGene).value.toPlainString()}\""
                    DynamoDbScalarTypeDto.BOOLEAN -> (attribute.gene as BooleanGene).value.toString()
                }
                val method = when (attribute.type) {
                    DynamoDbScalarTypeDto.STRING -> "s"
                    DynamoDbScalarTypeDto.NUMBER -> "n"
                    DynamoDbScalarTypeDto.BOOLEAN -> "bool"
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

    private fun escape(value: String, format: OutputFormat): String =
        StringEscapeUtils.escapeJava(value).let { if (format.isKotlin()) it.replace("$", "\\$") else it }
}
