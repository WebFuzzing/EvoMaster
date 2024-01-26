package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.search.EvaluatedDbAction
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.sql.SqlWrapperGene


/**
 * Class used to generate the code in the test dealing with insertion of
 * data into SQL databases.
 */
object SqlWriter {

    /**
     * generate sql insert actions into test case based on [dbInitialization]
     * @param format is the format of tests to be generated
     * @param dbInitialization contains the db actions to be generated
     * @param lines is used to save generated textual lines with respects to [dbInitialization]
     * @param allDbInitialization are all db actions in this test
     * @param groupIndex specifies an index of a group of this [dbInitialization]
     * @param insertionVars is a list of previous variable names of the db actions (Pair.first) and corresponding results (Pair.second)
     * @param skipFailure specifies whether to skip failure tests
     */
    fun handleDbInitialization(
        format: OutputFormat,
        dbInitialization: List<EvaluatedDbAction>,
        lines: Lines,
        allDbInitialization: List<SqlAction> = dbInitialization.map { it.sqlAction },
        groupIndex: String ="",
        insertionVars: MutableList<Pair<String, String>>,
        skipFailure: Boolean) {

        if (dbInitialization.isEmpty() || dbInitialization.none { !it.sqlAction.representExistingData && (!skipFailure || it.sqlResult.getInsertExecutionResult())}) {
            return
        }

        val insertionVar = "insertions${groupIndex}"
        val insertionVarResult = "${insertionVar}result"
        val previousVar = insertionVars.joinToString(", ") { it.first }
        val previousVarResults = insertionVars.joinToString(", ") { it.second }
        dbInitialization
                .filter { !it.sqlAction.representExistingData && (!skipFailure || it.sqlResult.getInsertExecutionResult())}
                .forEachIndexed { index, evaluatedDbAction ->

                    lines.add(when {
                        index == 0 && format.isJava() -> "List<InsertionDto> $insertionVar = sql($previousVar)"
                        index == 0 && format.isKotlin() -> "val $insertionVar = sql($previousVar)"
                        else -> ".and()"
                    } + ".insertInto(\"${evaluatedDbAction.sqlAction.table.name}\", ${evaluatedDbAction.sqlAction.geInsertionId()}L)")

                    if (index == 0) {
                        lines.indent()
                    }

                    lines.indented {
                        evaluatedDbAction.action.seeTopGenes()
                                .filter { it.isPrintable() }
                                .forEach { g ->
                                    when {
                                        g is SqlWrapperGene && g.getForeignKey() != null -> {
                                            val line = handleFK(format, g.getForeignKey()!!, evaluatedDbAction.sqlAction, allDbInitialization)
                                            lines.add(line)
                                        }
                                        g is ObjectGene -> {
                                            val variableName = g.getVariableName()
                                            val printableValue = getPrintableValue(format, g)
                                            lines.add(".d(\"$variableName\", \"'$printableValue'\")")
                                        }
                                        else -> {
                                            val variableName = g.getVariableName()
                                            val printableValue = getPrintableValue(format, g)
                                            lines.add(".d(\"$variableName\", \"$printableValue\")")
                                        }
                                    }
                                }

                    }
                }

        lines.add(".dtos()")
        lines.appendSemicolon(format)

        lines.deindent()

        lines.add(when{
            format.isJava() -> "InsertionResultsDto "
            format.isKotlin() -> "val "
            else -> throw IllegalStateException("Not support sql insertions generation for $format")
        } + "$insertionVarResult = controller.execInsertionsIntoDatabase(${if (previousVarResults.isBlank()) insertionVar else "$insertionVar, $previousVarResults"})")
        lines.appendSemicolon(format)

        insertionVars.add(insertionVar to insertionVarResult)

    }

    private fun getPrintableValue(format: OutputFormat, g: Gene): String {
        if (g is SqlPrimaryKeyGene) {
            return getPrintableValue(format, g.gene)

        } else {
            val x = g.getValueAsPrintableString(targetFormat = format)
            if(x.contains("\\\\")){
                //TODO already escaped???
                return x.replace("\"","\\\"")
            }
            return StringEscapeUtils.escapeJava(x)
            //TODO this is an atypical treatment of escapes. Should we run all escapes through the same procedure?
            // or is this special enough to be justified?
            /*
                FIXME: Yep, escaping in EM is currently a total mess... will need to be refactored/cleaned up
             */
        }
    }

    private fun handleFK(format: OutputFormat, fkg: SqlForeignKeyGene, action: SqlAction, allActions: List<SqlAction>): String {


        /*
            TODO: why the code here is not relying on SqlForeignKeyGene#getValueAsPrintableString ???
         */

        val variableName = fkg.getVariableName()
        /**
         * At this point all pk Ids should be valid
         * (despite they being NULL or not)
         **/
        Lazy.assert { fkg.hasValidUniqueIdOfPrimaryKey() }
        if (fkg.isNull()) {
            return ".d(\"$variableName\", \"NULL\")"
        }


        val uniqueIdOfPrimaryKey = fkg.uniqueIdOfPrimaryKey

        /*
            TODO: the code here is not handling multi-column PKs/FKs
         */
        val pkExisting = allActions
                .filter { it.representExistingData }
                .flatMap { it.seeTopGenes() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .find { it.uniqueId == uniqueIdOfPrimaryKey }

        /*
           This FK might point to a PK of data already existing in the database.
           In such cases, the PK will not be part of the generated SQL commands, and
           we cannot use a "r()" reference to it.
           We need to put the actual value data in a "d()"
        */

        if (pkExisting != null) {
            val pk = getPrintableValue(format, pkExisting)
            return ".d(\"$variableName\", \"$pk\")"
        }

        /*
            Check if this is a reference to an auto-increment
         */
        val keepAutoGeneratedValue = action.selectedColumns
                .first { it.name == fkg.name }
                .foreignKeyToAutoIncrement

        if (keepAutoGeneratedValue) {
            return ".r(\"$variableName\", ${uniqueIdOfPrimaryKey}L)"
        }


        val pkg = allActions
                .flatMap { it.seeTopGenes() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .find { it.uniqueId == uniqueIdOfPrimaryKey }!!

        val pk = getPrintableValue(format, pkg)
        return ".d(\"$variableName\", \"$pk\")"
    }

}