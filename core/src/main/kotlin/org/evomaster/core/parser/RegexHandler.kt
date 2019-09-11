package org.evomaster.core.parser

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.core.search.gene.regex.RegexGene


/**
 * Utility functions used to create RegexGene for different regex grammars.
 *
 * The parers/lexers for the grammars are generated with Antlr4 Maven Plugin,
 * as part of the build.
 * So, first time opened in an IDE like IntelliJ, need to run a "mvn compile"
 * first to generate those needed files.
 */
object RegexHandler {

    fun createGeneForJVM(regex: String) : RegexGene {

        val stream = CharStreams.fromString(regex)
        val lexer = RegexJavaLexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = RegexJavaParser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GeneRegexJavaVisitor().visit(pattern)

        return res.genes.first() as RegexGene
    }

    /**
     * Given a ECMA262 regex string, generate RegexGene for it.
     * Based on RegexEcma262.g4 file.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGeneForEcma262(regex: String): RegexGene {

        val stream = CharStreams.fromString(regex)
        val lexer = RegexEcma262Lexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = RegexEcma262Parser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GeneRegexEcma262Visitor().visit(pattern)

        return res.genes.first() as RegexGene
    }

    /**
     * Given a Postgres LIKE string constraint, generate RegexGene for it.
     * Based on PostgresLike.g4 file.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGeneForPostgresLike(regex: String): RegexGene {

        val stream = CharStreams.fromString(regex)
        val lexer = PostgresLikeLexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = PostgresLikeParser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GenePostgresLikeVisitor().visit(pattern)

        return res.genes.first() as RegexGene
    }

    /**
     * Given a Postgres SIMILAR TO string constraint, generate RegexGene for it.
     * Based on PostgresSimilarTo.g4 file.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGeneForPostgresSimilarTo(regex: String): RegexGene {

        val stream = CharStreams.fromString(regex)
        val lexer = PostgresSimilarToLexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = PostgresSimilarToParser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GenePostgresSimilarToVisitor().visit(pattern)

        return res.genes.first() as RegexGene
    }

    private fun prepareLexer(lexer: Lexer) : CommonTokenStream{
        lexer.removeErrorListeners()
        lexer.addErrorListener(ThrowingErrorListener())
        return CommonTokenStream(lexer)
    }

    private fun prepareParser(parser: Parser){
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingErrorListener())
    }

    /**
     * see https://stackoverflow.com/questions/18132078/handling-errors-in-antlr4
     */
    private class ThrowingErrorListener : BaseErrorListener() {

        override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException) {
            throw ParseCancellationException("line $line:$charPositionInLine $msg")
        }
    }

}