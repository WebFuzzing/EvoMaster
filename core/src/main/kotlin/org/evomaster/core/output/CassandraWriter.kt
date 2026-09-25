package org.evomaster.core.output

import org.apache.commons.text.StringEscapeUtils
import org.evomaster.core.database.cassandra.CassandraLiteralRenderer
import org.evomaster.core.search.action.EvaluatedCassandraDbAction

/**
 * Class used to generate the code in the test dealing with insertion of
 * data into CASSANDRA databases.
 */
object CassandraWriter {

    /**
     * generate cassandra insert actions into test case based on [cassandraDbInitialization]
     * @param format is the format of tests to be generated
     * @param cassandraDbInitialization contains the db actions to be generated
     * @param lines is used to save generated textual lines with respects to [cassandraDbInitialization]
     * @param groupIndex specifies an index of a group of this [cassandraDbInitialization]
     * @param skipFailure specifies whether to skip failure tests
     *
     * Unlike [SqlWriter], [MongoWriter] and [RedisWriter], no list of the insertion variables written
     * so far is taken: see below, none of them can be passed on to the Cassandra DSL, and the variable
     * generated here is not referenced by anything written afterwards either.
     */
    fun handleCassandraDbInitialization(
        format: OutputFormat,
        cassandraDbInitialization: List<EvaluatedCassandraDbAction>,
        lines: Lines,
        groupIndex: String = "",
        skipFailure: Boolean
    ) {

        if (cassandraDbInitialization.isEmpty()
            || cassandraDbInitialization.none { !skipFailure || it.cassandraResult.getInsertExecutionResult() }) {
            return
        }

        val insertionVar = "insertions_cassandra${groupIndex}"
        val insertionVarResult = "${insertionVar}_result"

        /*
            The DSL is always started with a bare cassandra(), never with the variables of the
            insertions written before in the same test. Unlike the SQL one, which checks the ids
            referenced by a foreign key against the previous insertions, the Cassandra DSL has no use
            for them, as there is no reference between rows in Cassandra.
         */
        cassandraDbInitialization
            .filter { !skipFailure || it.cassandraResult.getInsertExecutionResult() }
            .forEachIndexed { index, evaluatedCassandraDbAction ->

                lines.add(
                    when {
                        index == 0 && format.isJava() -> "List<CassandraInsertionDto> $insertionVar = cassandra()"
                        index == 0 && format.isKotlin() -> "val $insertionVar = cassandra()"
                        else -> ".and()"
                    } + ".insertInto(\"${evaluatedCassandraDbAction.cassandraAction.keyspace}\"" + ", " +
                            "\"${evaluatedCassandraDbAction.cassandraAction.table}\")"
                )

                if (index == 0) {
                    lines.indent()
                }

                lines.indented {
                    evaluatedCassandraDbAction.action.seeTopGenes()
                        .filter { it.isPrintable() }
                        .forEach { g ->
                            val printableValue = escape(CassandraLiteralRenderer.toCqlLiteral(g), format)
                            lines.add(".d(\"${g.name}\", \"$printableValue\")")
                        }
                }
            }

        lines.add(".dtos()")
        lines.appendSemicolon()

        lines.deindent()

        lines.add(
            when {
                format.isJava() -> "CassandraInsertionResultsDto "
                format.isKotlin() -> "val "
                else -> throw IllegalStateException("Not support cassandra insertions generation for $format")
            } + "$insertionVarResult = controller.execInsertionsIntoCassandraDatabase($insertionVar)"
        )
        lines.appendSemicolon()
    }

    /**
     * The CQL literal is embedded in a string literal of the generated test, so it has to be
     * escaped for the language such a test is written in.
     */
    private fun escape(value: String, format: OutputFormat): String {
        return StringEscapeUtils.escapeJava(value).let {
            if (format.isKotlin()) it.replace("$", "\\$") else it
        }
    }
}
