package org.evomaster.core.output

import java.nio.Buffer


/**
 * Class used to create an indented version of a list of strings, each
 * one representing a line
 */
class Lines {

    private val buffer: MutableList<String> = mutableListOf()

    var indentation = 0
        private set

    //TODO what about C#???
    fun shouldUseSemicolon(format: OutputFormat) = format.isJava() || format.isJavaScript() || format.isCsharp()

    fun appendSemicolon(format: OutputFormat) {

        if (shouldUseSemicolon(format)) {
            append(";")
        }
    }

    fun addStatement(statement: String, format: OutputFormat) {
        add(statement)
        appendSemicolon(format)
    }

    fun block(indentention: Int = 1, expression: () -> Any){
        append(" {")
        indented(indentention, expression)
        add("}")
    }

    fun indented(times: Int = 1, expression: () -> Any){
        indent(times)
        expression.invoke()
        deindent(times)
    }

    fun indent(times: Int = 1) {
        indentation += times
    }

    fun deindent(times: Int = 1) {
        indentation -= times
        if (indentation < 0) {
            throw IllegalStateException("Cannot de-indent on no indentation")
        }
    }

    fun add(other: Lines){
        //note: this will be relative to the current indentation
        other.buffer.forEach{l -> this.add(l)}
    }

    fun add(line: String) {
        if(line.endsWith("\n")){
            throw IllegalArgumentException("Added strings for lines shouldn't end with '\\n'")
        }
        val spaces = 4
        buffer.add(padding(spaces * indentation) + line)
    }

    fun replaceInCurrent(regex: Regex, replacement: String){
        if(buffer.isEmpty()){
            return
        }

        buffer[buffer.lastIndex] = buffer.last().replace(regex, replacement)
    }

    /**
     * Is the current line just a comment // without any statement?
     */
    fun isCurrentACommentLine() : Boolean{
        if(buffer.isEmpty()){
            return false
        }
        return buffer.last().matches(Regex("^\\s*//.*$"))
    }

    fun currentContains(s: String) : Boolean{
        if(buffer.isEmpty()){
            return false
        }

        return buffer.last().contains(s)
    }

    /**
     * Add n=[times] lines that are empty
     */
    fun addEmpty(times: Int = 1) {
        if (times <= 0) {
            throw IllegalArgumentException("Invalid 'times' value: $times")
        }
        (1..times).forEach { add("") }
    }

    fun append(token: String) {
        buffer[buffer.lastIndex] = buffer.last() + token
    }

    override fun toString(): String {
        val s = StringBuffer(buffer.sumOf{it.length + 2})
        buffer.forEach { v -> s.append("$v\n") }
        return s.toString()
    }

    private fun padding(n: Int): String {

        if (n < 0) {
            throw IllegalArgumentException("Invalid n=$n")
        }

        val buffer = StringBuffer("")
        (1..n).forEach { buffer.append(" ") }
        return buffer.toString()
    }

}