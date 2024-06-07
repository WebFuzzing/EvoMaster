package org.evomaster.core.solver

import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.select.FromItem
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto
import org.evomaster.client.java.controller.api.dto.database.schema.ForeignKeyDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.solver.smtlib.*
import org.evomaster.solver.smtlib.assertion.DistinctAssertion
import org.evomaster.solver.smtlib.assertion.EqualsAssertion
import org.evomaster.solver.smtlib.assertion.OrAssertion
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
            val dataTypeName = "${capitalization(table.name)}Row"

            smt.addNode(
                DeclareDatatypeSMTNode(dataTypeName, getConstructors(table))
            )

            for (i in 1..numberOfRows) {
                smt.addNode(
                    DeclareConstSMTNode("${table.name.lowercase(Locale.getDefault())}$i", dataTypeName)
                )
            }
        }
    }

    private fun capitalization(word: String) =
        word.substring(0, 1).uppercase(Locale.getDefault()) +
        word.substring(1).lowercase(Locale.getDefault())

    private fun appendKeyConstraints(smt: SMTLib) {
        for (table in schema.tables) {
            appendPrimaryKeyConstraints(smt, table)
            appendForeignKeyConstraints(smt, table)
        }
    }

    private fun appendPrimaryKeyConstraints(smt: SMTLib, table: TableDto) {

        val tableName = table.name.lowercase(Locale.getDefault())
        val primaryKeys = table.columns.filter { it.primaryKey }

        for (primaryKey in primaryKeys) {
            val pkSelector = primaryKey.name.uppercase(Locale.getDefault())
            val nodes = assertForDistinctField(pkSelector, tableName)

            smt.addNodes(nodes)
        }
    }

    private fun assertForDistinctField(pkSelector: String, tableName: String): List<SMTNode> {
        val nodes = mutableListOf<AssertSMTNode>()
        for (i in 1..numberOfRows) {
            for (j in i + 1..numberOfRows) {
                nodes.add(
                    AssertSMTNode(
                        DistinctAssertion(
                            listOf(
                                "$pkSelector $tableName$i",
                                "$pkSelector $tableName$j"
                            )
                        )
                    )
                )
            }
        }
        return nodes
    }

    private fun appendForeignKeyConstraints(smt: SMTLib, table: TableDto) {
        val sourceTableName = table.name.lowercase(Locale.getDefault())

        for (foreignKey in table.foreignKeys) {
            val referencedTable = findReferencedTable(foreignKey)
            val referencedTableName = referencedTable.name.lowercase(Locale.getDefault())
            val referencedColumnSelector = findReferencedPKSelector(referencedTable, foreignKey)

            for (sourceColumn in foreignKey.sourceColumns) {
                val sourceColumnSelector = sourceColumn.uppercase(Locale.getDefault())

                val nodes = assertForEqualsAny(
                    sourceColumnSelector, sourceTableName,
                    referencedColumnSelector, referencedTableName)

                smt.addNodes(nodes)
            }
        }
    }

    private fun assertForEqualsAny(
        sourceColumnSelector: String, sourceTableName: String,
        referencedColumnSelector: String, referencedTableName: String
    ): List<AssertSMTNode> {
        val nodes = mutableListOf<AssertSMTNode>()

        for (i in 1..numberOfRows) {
            val conditions = (1..numberOfRows).map { j ->
                EqualsAssertion(
                    listOf(
                        "$sourceColumnSelector $sourceTableName$i",
                        "$referencedColumnSelector $referencedTableName$j"
                    )
                )
            }
            nodes.add(AssertSMTNode(OrAssertion(conditions)))
        }
        return nodes
    }

    private fun findReferencedPKSelector(referencedTable: TableDto, foreignKey: ForeignKeyDto): String {
        val referencedPrimaryKeys = referencedTable.columns.filter { it.primaryKey }
        if (referencedPrimaryKeys.isEmpty()) {
            throw RuntimeException("Referenced table has no primary key: ${foreignKey.targetTable}")
        }
        // Assuming single-column primary keys
        return referencedPrimaryKeys[0].name.uppercase(Locale.getDefault())
    }

    private fun findReferencedTable(foreignKey: ForeignKeyDto): TableDto {
        return schema.tables.firstOrNull { it.name.equals(foreignKey.targetTable, ignoreCase = true) }
            ?: throw RuntimeException("Referenced table not found: ${foreignKey.targetTable}")
    }

    private fun appendQueryConstraints(smt: SMTLib, selectStatement: Statement) {

        val tableAliases = extractTableAliases(selectStatement)
        val condition = extractCondition(selectStatement) ?: return

        val tables = extractTableNames(selectStatement).map { tableName ->
            schema.tables.find { it.name.equals(tableName, ignoreCase = true) }
                ?: throw RuntimeException("Table not found in schema: $tableName")
        }

        tables.forEach { table ->
            (1..numberOfRows).forEach { i ->
                val rowVariable = "${table.name.lowercase(Locale.getDefault())}$i"
                // TODO: handle joins
            }
        }
    }

    private fun extractTableNames(statement: Statement): List<String> {
        val tableNames = mutableListOf<String>()

        val select = statement as Select
        val plainSelect = select.selectBody as PlainSelect

        fun extractTableName(fromItem: FromItem) {
            val tableName = when (fromItem) {
                is Table -> fromItem.name
                else -> null
            }
            if (tableName != null) {
                tableNames.add(tableName)
            }
        }

        extractTableName(plainSelect.fromItem)

        plainSelect.joins?.forEach { join ->
            extractTableName(join.rightItem)
        }

        return tableNames
    }

    private fun extractTableAliases(statement: Statement): Map<String, String> {
        val tableAliases = mutableMapOf<String, String>()

        val select = statement as Select
        val plainSelect = select.selectBody as PlainSelect

        fun extractAlias(fromItem: FromItem) {
            val alias = fromItem.alias
            val tableName = when (fromItem) {
                is Table -> fromItem.name
                else -> null
            }
            if (alias != null && tableName != null) {
                tableAliases[alias.name] = tableName
            }
        }

        extractAlias(plainSelect.fromItem)

        plainSelect.joins?.forEach { join ->
            extractAlias(join.rightItem)
        }

        return tableAliases
    }

    private fun extractCondition(statement: Statement): Expression? {
        val select = statement as Select
        val plainSelect = select.selectBody as PlainSelect
        return plainSelect.where
    }

    private fun appendGetValues(smt: SMTLib) {
        smt.addNode(CheckSatSMTNode())

        for (table in schema.tables) {
            val tableNameLower = table.name.lowercase(Locale.getDefault())
            for (i in 1..numberOfRows) {
                smt.addNode(GetValueSMTNode("$tableNameLower$i"))
            }
        }
    }

    private fun getConstructors(table: TableDto): List<DeclareConstSMTNode> {
        return table.columns.map { c ->
            val smtType = TYPE_MAP[c.type.uppercase(Locale.getDefault())]
            DeclareConstSMTNode(c.name, smtType!!)
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
