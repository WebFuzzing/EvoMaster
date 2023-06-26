package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.search.EvaluatedMongoDbAction
import org.evomaster.core.search.gene.ObjectGene

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
        skipFailure: Boolean) {

        if (mongoDbInitialization.isEmpty()) return

        val insertionVar = "insertions${groupIndex}"
        val insertionVarResult = "${insertionVar}result"
        val previousVar = insertionVars.joinToString(", ") { it.first }
        val previousVarResults = insertionVars.joinToString(", ") { it.second }
        mongoDbInitialization
                .filter { !skipFailure || it.result.getInsertExecutionResult()}
                .forEachIndexed { index, evaluatedMongoDbAction ->

                    lines.add(when {
                        index == 0 && format.isJava() -> "List<MongoInsertionDto> $insertionVar = mongo($previousVar)"
                        index == 0 && format.isKotlin() -> "val $insertionVar = mongo($previousVar)"
                        else -> ".and()"
                    } + ".insertInto(\"${evaluatedMongoDbAction.action.database}\"" + ", " + "\"${evaluatedMongoDbAction.action.collection}\")")

                    if (index == 0) {
                        lines.indent()
                    }

                    lines.indented {
                        evaluatedMongoDbAction.action.seeTopGenes()
                                .filter { it.isPrintable() }
                                .forEach { g ->
                                    when (g) {
                                        is ObjectGene -> {
                                            val printableValue = StringEscapeUtils.escapeJava(g.getValueAsPrintableString())
                                            lines.add(".d(\"$printableValue\")")
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

        lines.add(when{
            format.isJava() -> "MongoInsertionResultsDto "
            format.isKotlin() -> "val "
            else -> throw IllegalStateException("Not support mongo insertions generation for $format")
        } + "$insertionVarResult = controller.execInsertionsIntoMongoDatabase(${if (previousVarResults.isBlank()) insertionVar else "$insertionVar, $previousVarResults"})")
        lines.appendSemicolon(format)

        insertionVars.add(insertionVar to insertionVarResult)

    }

}