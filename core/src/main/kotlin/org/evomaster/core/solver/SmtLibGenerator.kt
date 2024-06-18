package org.evomaster.core.solver

import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.controller.api.dto.database.schema.*
import org.evomaster.core.utils.StringUtils
import org.evomaster.dbconstraint.ConstraintDatabaseType
import org.evomaster.dbconstraint.ast.SqlCondition
import org.evomaster.dbconstraint.parser.SqlConditionParserException
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser
import org.evomaster.solver.smtlib.*
import org.evomaster.solver.smtlib.assertion.Assertion
import org.evomaster.solver.smtlib.assertion.DistinctAssertion
import org.evomaster.solver.smtlib.assertion.EqualsAssertion
import org.evomaster.solver.smtlib.assertion.OrAssertion
import java.util.*


class SmtLibGenerator(private val schema: DbSchemaDto, private val numberOfRows: Int) {

    private var parser = JSqlConditionParser()

    fun generateSMT(sqlQuery: Statement): SMTLib {
        val smt = SMTLib()

        appendTableDefinitions(smt)
        appendTableConstraints(smt)
        appendKeyConstraints(smt)
        appendQueryConstraints(smt, sqlQuery)
        appendGetValues(smt)

        return smt
    }

    private fun appendTableDefinitions(smt: SMTLib) {
        for (table in schema.tables) {
            val dataTypeName = "${StringUtils.capitalization(table.name)}Row"

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

    private fun appendTableConstraints(smt: SMTLib) {
        for (table in schema.tables) {
            appendUniqueConstraints(smt, table)
            appendCheckConstraints(smt, table)
        }
    }

    private fun appendUniqueConstraints(smt: SMTLib, table: TableDto) {
        val tableName = table.name.lowercase(Locale.getDefault())
        for (column in table.columns) {
            if (column.unique) {
                val columnName = column.name.uppercase(Locale.getDefault())
                val nodes = assertForDistinctField(columnName, tableName)
                smt.addNodes(nodes)
            }
        }
    }

    private fun appendCheckConstraints(smt: SMTLib, table: TableDto) {
        for (check in table.tableCheckExpressions) {
            try {
                val condition: SqlCondition = parser.parse(check.sqlCheckExpression, toDBType(schema.databaseType))
                for (i in 1..numberOfRows) {
                    val constraint: SMTNode = parseCheckExpression(table, condition, i)
                    smt.addNode(constraint)
                }
            } catch (e: SqlConditionParserException) {
                throw java.lang.RuntimeException("Error parsing check expression: " + check.sqlCheckExpression, e)
            }
        }
    }

    private fun parseCheckExpression(table: TableDto, condition: SqlCondition, index: Int): SMTNode {
        val visitor = SMTConditionVisitor(table.name.lowercase(Locale.getDefault()), index)
        return condition.accept(visitor, null) as SMTNode
    }

    private fun toDBType(databaseType: DatabaseType?): ConstraintDatabaseType {
        return when (databaseType) {
            DatabaseType.H2 -> ConstraintDatabaseType.H2
            DatabaseType.DERBY -> ConstraintDatabaseType.DERBY
            DatabaseType.MYSQL -> ConstraintDatabaseType.MYSQL
            DatabaseType.POSTGRES -> ConstraintDatabaseType.POSTGRES
            DatabaseType.MARIADB -> ConstraintDatabaseType.MARIADB
            DatabaseType.MS_SQL_SERVER -> ConstraintDatabaseType.MS_SQL_SERVER
            else -> ConstraintDatabaseType.OTHER
        }
    }

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
                                "($pkSelector $tableName$i)",
                                "($pkSelector $tableName$j)"
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
                        "($sourceColumnSelector $sourceTableName$i)",
                        "($referencedColumnSelector $referencedTableName$j)"
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
        // TODO
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
