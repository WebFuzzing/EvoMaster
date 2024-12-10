package org.evomaster.core.search.service

import com.google.inject.Inject
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.util.deparser.SelectDeParser
import net.sf.jsqlparser.util.deparser.StatementDeParser
import org.evomaster.client.java.controller.api.dto.ExtraHeuristicEntryDto
import org.evomaster.core.EMConfig
import org.evomaster.core.sql.ReplaceValuesDeParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct


/**
 * Service used to write to disk info on the extra heuristics
 */
class ExtraHeuristicsLogger {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var time: SearchTimeController

    @PostConstruct
    private fun postConstruct() {
        if (config.writeExtraHeuristicsFile) {
            val extraHeuristicsFilePath = getExtraHeuristicsFilePath()
            setUpFilePath(extraHeuristicsFilePath)
            extraHeuristicsFilePath.toFile().appendText("testCounter,actionId,value,id,type,objective,group\n")
        }
    }

    /**
     * Creates parent path if it is not exists.
     * And deletes the file if the file already exists.
     */
    private fun setUpFilePath(filePath: Path) {
        Files.createDirectories(filePath.parent)
        Files.deleteIfExists(filePath)
        Files.createFile(filePath)
    }

    private fun getExtraHeuristicsFilePath() = Paths.get(config.extraHeuristicsFile).toAbsolutePath()

    fun writeHeuristics(list: List<ExtraHeuristicEntryDto>, actionId: Int) {
        if (!config.writeExtraHeuristicsFile) {
            return
        }

        val counter = time.evaluatedIndividuals
        val lines = list.map {
            val group = if (it.type == ExtraHeuristicEntryDto.Type.SQL) {
                try {
                    "\"${removeAllConstants(it.id)}\""
                } catch (ex: JSQLParserException) {
                    "JSQLParserException: ${ex.message}"
                }
            } else
                "-"
            "$counter,$actionId,${it.value},\"${it.id}\",${it.type},${it.objective},$group\n"
        }.joinToString("")

        getExtraHeuristicsFilePath().toFile().appendText(lines)

    }

    /**
     * Remove all constants from the SQL commands (eg, strings and numbers),
     * to group together command with same structure, regardless of the used variables.
     *
     */
    fun removeAllConstants(sql: String): String {

        val buffer = StringBuilder()
        val expressionDeParser = ReplaceValuesDeParser()
        val selectDeparser = SelectDeParser(expressionDeParser, buffer)
        expressionDeParser.selectVisitor = selectDeparser
        expressionDeParser.buffer = buffer

        val stmtDeparser = StatementDeParser(expressionDeParser, selectDeparser, buffer)
        val stmt = CCJSqlParserUtil.parse(sql)
        stmt.accept(stmtDeparser)
        return stmtDeparser.buffer.toString()

    }
}
