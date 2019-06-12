package org.evomaster.core.parser.visitor

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.core.parser.RegexEcma262Lexer
import org.evomaster.core.parser.RegexEcma262Parser
import org.evomaster.core.search.gene.regex.RegexGene


/**
 * Regex functions based on the JavaScript ECMA262 grammar,
 * defined in the RegexEcma262.g4 file.
 * The parer/lexer for the grammar is generated with Antlr4 Maven Plugin,
 * as part of the build.
 */
object Ecma262Handler {

    /**
     * Given a regex string, generate RegexGene for it.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGene(regex: String): RegexGene {

        val stream = CharStreams.fromString(regex)
        val lexer = RegexEcma262Lexer(stream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(ThrowingErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = RegexEcma262Parser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingErrorListener())

        val pattern = parser.pattern()

        val res = Ecma262Visitor().visit(pattern)

        return res.genes.first() as RegexGene

    }


    //see https://stackoverflow.com/questions/18132078/handling-errors-in-antlr4
    private class ThrowingErrorListener : BaseErrorListener() {

        override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException) {
            throw ParseCancellationException("line $line:$charPositionInLine $msg")
        }
    }

}