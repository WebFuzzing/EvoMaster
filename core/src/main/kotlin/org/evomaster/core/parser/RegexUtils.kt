package org.evomaster.core.parser

import java.util.stream.Collectors


object RegexUtils {

    fun ignoreCaseRegex(input: String) : String {

        return input.chars()
                .mapToObj{
                    val c = it.toChar()
                    val l = Character.toLowerCase(c)
                    val u = Character.toUpperCase(c)
                    //characters could be control in regex, so need escaped inside quote
                    if(l != u) "($l|$u)" else "\\Q$l\\E"
                }
                .collect(Collectors.joining())
                // a chain of quotes can be merged into a single one
                .replace("\\E\\Q", "")
    }

}