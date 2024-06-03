package org.evomaster.core.solver

import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.solver.smtlib.*
import java.util.*


class SmtLibGenerator(private val schema: DbSchemaDto, private val numberOfRows: Int) {

    fun generateSMT(sqlQuery: Statement): SMTLib {
        val smt = SMTLib()

        appendTableDefinitions(smt)
        appendKeyConstraints(smt)
        appendQueryConstraints(smt, sqlQuery)
        appendGetValues(smt)

        return smt
    }

    private fun appendTableDefinitions(smt: SMTLib) {
        for (table in schema.tables) {
            val tableName = table.name.substring(0, 1).uppercase(Locale.getDefault()) + table.name.substring(1).toLowerCase()
            val dataTypeName = tableName + "Row"
            smt.addNode(DeclareDatatype(dataTypeName, getConstructors(table)))

            for (i in 1..numberOfRows) {
                smt.addNode(DeclareConst("${table.name.lowercase(Locale.getDefault())}$i", dataTypeName))
            }
        }
    }

    private fun appendKeyConstraints(smt: SMTLib) {
        // TODO
    }

    private fun appendQueryConstraints(smt: SMTLib, sqlQuery: Statement) {
        // TODO
    }

    private fun appendGetValues(smt: SMTLib) {
        smt.addNode(CheckSat())

        for (table in schema.tables) {
            val tableNameLower = table.name.lowercase(Locale.getDefault())
            for (i in 1..numberOfRows) {
                smt.addNode(GetValue("$tableNameLower$i"))
            }
        }
    }

    private fun getConstructors(table: TableDto): List<DeclareConst> {
        return table.columns.map { c ->
            val smtType = TYPE_MAP[c.type.uppercase(Locale.getDefault())]
            DeclareConst(c.name, smtType!!)
        }
    }

    companion object {
        private val TYPE_MAP = mapOf(
            "BIGINT" to "Int",
            "INTEGER" to "Int",
            "FLOAT" to "Real",
            "DOUBLE" to "Real",
            "CHARACTER VARYING" to "String",
            "CHAR" to "String"
        )
    }
}
