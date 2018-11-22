package org.evomaster.core.output.formatter

class MismatchedFormatException(
        formatter: OutputFormatter,
        msg: String
) : Exception("${formatter.name} cannot format $msg")