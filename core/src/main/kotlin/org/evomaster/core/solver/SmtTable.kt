package org.evomaster.core.solver

import org.evomaster.core.sql.schema.Table
import org.evomaster.core.utils.StringUtils
import org.evomaster.core.utils.StringUtils.convertToAscii

/**
 * A view of a [Table] with pre-computed SMT-safe identifiers.
 *
 * All table and column names are converted to ASCII once at construction time,
 * avoiding repeated [convertToAscii] calls throughout [SmtLibGenerator].
 *
 * @param table The domain-level table object used for schema metadata.
 * @param checkExpressions Raw SQL strings for CHECK constraints, preserved separately
 *   because [Table.tableConstraints] uses the [org.evomaster.dbconstraint.TableConstraintVisitor]
 *   hierarchy, while [SmtLibGenerator] parses constraints via
 *   [org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser] into
 *   [org.evomaster.dbconstraint.ast.SqlCondition] objects for [SMTConditionVisitor].
 */
class SmtTable(val table: Table, val checkExpressions: List<String>) {

    /** Original lowercase table name, used to match against SQL query table references. */
    val originalName: String = table.id.name.lowercase()

    /** SMT-safe lowercase identifier used in row constant declarations (e.g., "person1", "person2"). */
    val smtName: String = convertToAscii(table.id.name).lowercase()

    /** SMT-LIB datatype name for this table's rows (e.g., "PersonRow"). */
    val dataTypeName: String = "${StringUtils.capitalization(smtName)}Row"

    private val columnSmtNames: Map<String, String> =
        table.columns.associate { col -> col.name to convertToAscii(col.name) }

    /** Returns the SMT-safe identifier for the given column name. */
    fun smtColumnName(columnName: String): String =
        columnSmtNames[columnName] ?: convertToAscii(columnName)
}
