package org.evomaster.core.sql

import net.sf.jsqlparser.expression.*
import net.sf.jsqlparser.util.deparser.ExpressionDeParser

/**
 * Replaces all constant values in a SQL query with a '?' symbol.
 * For example,
 * SELECT * FROM FooTable WHERE barColumn='Hello World'
 * is translated into
 * SELECT * FROM FooTable WHERE barColumn=?
 *
 * This transformation is used to "abstract" queries with the
 * same structure (Tables, joins, columns) but different
 * queried constant values.
 */
class ReplaceValuesDeParser() :
    ExpressionDeParser() {

    private fun appendSymbol() {
        getBuffer().append("?")
    }

    override fun visit(stringValue: StringValue?) {
        appendSymbol()
    }

    override fun visit(longValue: LongValue?) {
        appendSymbol()
    }

    override fun visit(doubleValue: DoubleValue?) {
        appendSymbol()
    }

    override fun visit(hexValue: HexValue?) {
        appendSymbol()
    }

    override fun visit(dateValue: DateValue?) {
        appendSymbol()
    }

    override fun visit(timestampValue: TimestampValue?) {
        appendSymbol()
    }

    override fun visit(timeValue: TimeValue?) {
        appendSymbol()
    }

    override fun visit(dateTimeLiteralExpression: DateTimeLiteralExpression?) {
        appendSymbol()
    }

}