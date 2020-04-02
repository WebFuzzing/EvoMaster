package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
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

    fun handleDbInitialization(format: OutputFormat, dbInitialization: List<DbAction>, lines: Lines) {

        if (dbInitialization.isEmpty() || dbInitialization.none { !it.representExistingData }) {
            return
        }

        dbInitialization
                .filter { !it.representExistingData }
                .forEachIndexed { index, dbAction ->

                    lines.add(when {
                        index == 0 && format.isJava() -> "List<InsertionDto> insertions = sql()"
                        index == 0 && format.isKotlin() -> "val insertions = sql()"
                        else -> ".and()"
                    } + ".insertInto(\"${dbAction.table.name}\", ${dbAction.geInsertionId()}L)")

                    if (index == 0) {
                        lines.indent()
                    }

                    lines.indented {
                        dbAction.seeGenes()
                                .filter { it.isPrintable() }
                                .forEach { g ->
                                    when {
                                        g is SqlWrapperGene && g.getForeignKey() != null -> {
                                            val line = handleFK(format, g.getForeignKey()!!, dbAction, dbInitialization)
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

        lines.add("controller.execInsertionsIntoDatabase(insertions)")
        lines.appendSemicolon(format)
    }

    private fun getPrintableValue(format: OutputFormat, g: Gene): String {
        if (g is SqlPrimaryKeyGene) {
            return getPrintableValue(format, g.gene)

        } else {
            return StringEscapeUtils.escapeJava(g.getValueAsPrintableString(targetFormat = format))
            //TODO this is an atypical treatment of escapes. Should we run all escapes through the same procedure?
            // or is this special enough to be justified?
        }
    }

    private fun handleFK(format: OutputFormat, fkg: SqlForeignKeyGene, action: DbAction, allActions: List<DbAction>): String {


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
                .flatMap { it.seeGenes() }
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
                .flatMap { it.seeGenes() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .find { it.uniqueId == uniqueIdOfPrimaryKey }!!

        val pk = getPrintableValue(format, pkg)
        return ".d(\"$variableName\", \"$pk\")"
    }

}