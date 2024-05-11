package org.evomaster.core.parser

import org.antlr.v4.runtime.ParserRuleContext
import java.util.stream.Collectors


object RegexUtils {

    /**
     * These are regex with no value, as they match everything.
     * Note: we could have something more sophisticated, to check for any possible meaningless one.
     * But this simple list should do for most cases.
     *
     * TODO: this is not really true, as by default . does not match line breakers like \n
     * So, although they are not important, they are technically not "meaningless"
     */
    private val meaninglessRegex = setOf(".*","(.*)","^(.*)","(.*)$","^(.*)$","^((.*))","((.*))$","^((.*))$")

    /**
     * Coming from widely known framework, eg favicon.ico in Spring on controller matching
     */
    private val knownUselessRegex = setOf("\\Qfavicon.ico\\E")

    fun isMeaningfulRegex(regex: String):  Boolean {
        return ! meaninglessRegex.contains(regex)
    }

    fun isNotUselessRegex(regex: String) : Boolean{
        return !knownUselessRegex.contains(regex)
    }

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

    /**
     * @return regex expression in string format based on [ctx]
     */
    fun getRegexExpByParserRuleContext(ctx : ParserRuleContext) : String{
        return try {
            ctx.text
        }catch (e : Exception){ // avoid any problem due to retrieval of additional info
            ""
        }
    }

}