package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.search.EvaluatedMongoDbAction
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.utils.GeneUtils

/**
 * Class used to generate the code in the test dealing with insertion of
 * data into MONGO databases.
 */
object MongoWriter {

    /**
     * generate mongo insert actions into test case based on [mongoDbInitialization]
     * @param format is the format of tests to be generated
     * @param mongoDbInitialization contains the db actions to be generated
     * @param lines is used to save generated textual lines with respects to [mongoDbInitialization]
     * @param groupIndex specifies an index of a group of this [mongoDbInitialization]
     * @param insertionVars is a list of previous variable names of the db actions (Pair.first) and corresponding results (Pair.second)
     * @param skipFailure specifies whether to skip failure tests
     */
    fun handleMongoDbInitialization(
        format: OutputFormat,
        mongoDbInitialization: List<EvaluatedMongoDbAction>,
        lines: Lines,
        groupIndex: String = "",
        insertionVars: MutableList<Pair<String, String>>,
        skipFailure: Boolean
    ) {

        if (mongoDbInitialization.isEmpty() || mongoDbInitialization.none {!skipFailure || it.mongoResult.getInsertExecutionResult()}) {
            return
        }

        val insertionVar = "insertions${groupIndex}"
        val insertionVarResult = "${insertionVar}result"
        val previousVar = insertionVars.joinToString(", ") { it.first }
        mongoDbInitialization
            .filter { !skipFailure || it.mongoResult.getInsertExecutionResult() }
            .forEachIndexed { index, evaluatedMongoDbAction ->

                lines.add(
                    when {
                        index == 0 && format.isJava() -> "List<MongoInsertionDto> $insertionVar = mongo($previousVar)"
                        index == 0 && format.isKotlin() -> "val $insertionVar = mongo($previousVar)"
                        else -> ".and()"
                    } + ".insertInto(\"${evaluatedMongoDbAction.mongoAction.database}\"" + ", " + "\"${evaluatedMongoDbAction.mongoAction.collection}\")"
                )

                if (index == 0) {
                    lines.indent()
                }

                lines.indented {
                    evaluatedMongoDbAction.action.seeTopGenes()
                        .filter { it.isPrintable() }
                        .forEach { g ->
                            when (g) {
                                is ObjectGene -> {
                                    val printableValue =
                                        StringEscapeUtils.escapeJava(g.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON))
                                    val adaptedPrintableValue = if (format.isKotlin())
                                        printableValue.replace(
                                            "$",
                                            "\\$"
                                        )
                                    else printableValue
                                    lines.add(".d(\"$adaptedPrintableValue\")")
                                }

                                else -> {
                                }
                            }
                        }

                }
            }

        lines.add(".dtos()")
        lines.appendSemicolon(format)

        lines.deindent()

        lines.add(
            when {
                format.isJava() -> "MongoInsertionResultsDto "
                format.isKotlin() -> "val "
                else -> throw IllegalStateException("Not support mongo insertions generation for $format")
            } + "$insertionVarResult = controller.execInsertionsIntoMongoDatabase($insertionVar)"
        )
        lines.appendSemicolon(format)

        insertionVars.add(insertionVar to insertionVarResult)

    }

}