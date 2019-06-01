package org.evomaster.core.parser.visitor

import org.antlr.v4.runtime.*
import org.evomaster.core.parser.RegexEcma262Lexer
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.parser.RegexEcma262Parser
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.core.remote.SutProblemException


object Ecma262Handler {

    fun createGene(regex: String) : RegexGene{

       // try {
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

//        } catch (e: ParseCancellationException){
//            throw SutProblemException("Invalid/not-supported regular expression: $regex")
//        }
    }


    //see https://stackoverflow.com/questions/18132078/handling-errors-in-antlr4
    private class ThrowingErrorListener : BaseErrorListener() {

        override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException) {
            throw ParseCancellationException("line $line:$charPositionInLine $msg")
        }
    }

}