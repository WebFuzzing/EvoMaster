package org.evomaster.core.solver

import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.utils.StringUtils
import org.evomaster.core.utils.StringUtils.convertToAscii

/**
 * A view of a [TableDto] with pre-computed SMT-safe identifiers.
 *
 * All table and column names are converted to ASCII once at construction time,
 * avoiding repeated [convertToAscii] calls throughout [SmtLibGenerator].
 */
class SmtTable(val dto: TableDto) {

    /** Original lowercase table name, used to match against SQL query table references. */
    val originalName: String = dto.id.name.lowercase()

    /** SMT-safe lowercase identifier used in row constant declarations (e.g., "person1", "person2"). */
    val smtName: String = convertToAscii(dto.id.name).lowercase()

    /** SMT-LIB datatype name for this table's rows (e.g., "PersonRow"). */
    val dataTypeName: String = "${StringUtils.capitalization(smtName)}Row"

    private val columnSmtNames: Map<String, String> =
        dto.columns.associate { col -> col.name to convertToAscii(col.name) }

    /** Returns the SMT-safe identifier for the given column name. */
    fun smtColumnName(columnName: String): String =
        columnSmtNames[columnName] ?: convertToAscii(columnName)
}
