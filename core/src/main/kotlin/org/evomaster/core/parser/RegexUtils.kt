package org.evomaster.core.parser

import java.util.stream.Collectors


object RegexUtils {

    fun ignoreCaseRegex(input: String) : String {

        return input.chars()
                .mapToObj{
                    val c = it.toChar()
                    val l = Character.toLowerCase(c)
                    val u = Character.toUpperCase(c)
                    if(l != u) "($l|$u)" else "$l"
                }
                .collect(Collectors.joining())
    }

}