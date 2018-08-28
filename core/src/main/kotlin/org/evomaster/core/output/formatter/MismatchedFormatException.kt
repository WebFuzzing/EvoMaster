package org.evomaster.core.output.formatter

class MismatchedFormatException(val formatter: OutputFormatter, val msg:String): Exception( formatter.name +" cannot format "+msg)