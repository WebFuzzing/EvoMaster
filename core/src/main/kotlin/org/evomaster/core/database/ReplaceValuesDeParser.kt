package org.evomaster.core.database

import net.sf.jsqlparser.expression.*
import net.sf.jsqlparser.util.deparser.ExpressionDeParser


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