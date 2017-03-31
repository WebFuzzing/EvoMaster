package org.evomaster.core.output


/**
 * Class used to created an indented version of a list of strings, each
 * one representing a line
 */
class Lines {

    val buffer: MutableList<String> = mutableListOf()

    var indentation = 0
        private set

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
        val s = StringBuffer(buffer.sumBy(String::length))
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