package org.evomaster.core.parser

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.utils.ParsedFlagExpression
import org.evomaster.core.utils.RegexFlags
import org.evomaster.core.utils.RegexWithExternalFlags


/**
 * Utility functions used to create RegexGene for different regex grammars.
 *
 * The parers/lexers for the grammars are generated with Antlr4 Maven Plugin,
 * as part of the build.
 * So, first time opened in an IDE like IntelliJ, need to run a "mvn compile"
 * first to generate those needed files.
 */
object RegexHandler {

    /*
        WARNING mutable static state, but those are just caches.
        Key -> regex
     */
    private val cacheJVM : MutableMap<RegexWithExternalFlags, RegexGene> = mutableMapOf()
    private val cacheEcma262 : MutableMap<String, RegexGene> = mutableMapOf()
    private val cachePostgresLike : MutableMap<String, RegexGene> = mutableMapOf()
    private val cachePostgresSimilarTo : MutableMap<String, RegexGene> = mutableMapOf()

    fun createGeneForJVM(regex: String, externalRegexFlags: RegexFlags = RegexFlags()) : RegexGene {

        val key = RegexWithExternalFlags(regex, externalRegexFlags)
        if(cacheJVM.contains(key)){
            return cacheJVM[key]!!.copy() as RegexGene
        }

        val preprocessedRegex = preprocessCommentsForJavaRegex(regex, externalRegexFlags)

        val stream = CharStreams.fromString(preprocessedRegex)
        val lexer = RegexJavaLexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = RegexJavaParser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GeneRegexJavaVisitor(externalRegexFlags).visit(pattern)

        val gene= res.genes.first() as RegexGene
        cacheJVM[key] = gene.copy() as RegexGene
        return gene
    }

    /**
     * This function handles comments and whitespace for Java regex, striping them when the "x" flag is on.
     *
     * This cannot be handled at the ANTLR level because the flag can be enabled and disabled
     * mid-pattern via inline flag groups like `(?x:...)` and `(?-x:...)`, which are only known
     * at parse time. Lexer modes cannot react to parser-level flag state.
     *
     * The visitor cannot handle this either, as some constructs (character classes, quantifier bounds, etc.)
     * are tokenised before the visitor sees them.
     *
     * This function therefore performs a linear scan of the raw string before ANTLR, tracking
     * flag state across inline scopes, producing a cleaned string that requires no special
     * handling in the lexer or visitor.
     */
    private fun preprocessCommentsForJavaRegex(regex: String, externalRegexFlags: RegexFlags): String {
        val result = StringBuilder(regex.length)
        val scopeStack = ArrayDeque<RegexFlags>() // stack of flags per level
        var currentFlags = externalRegexFlags
        var i = 0

        while (i < regex.length) {
            val c = regex[i]
            when {
                // backslash escape
                c == '\\' && i + 1 < regex.length -> {
                    when {
                        regex[i+1] == 'Q' -> {
                            // \Q...\E quote block, copy everything
                            result.append('\\'); result.append('Q')
                            i += 2
                            while (i < regex.length) {
                                if (regex[i] == '\\' && i+1 < regex.length && regex[i+1] == 'E') {
                                    result.append('\\'); result.append('E')
                                    i += 2; break
                                }
                                result.append(regex[i++])
                            }
                        }
                        else -> {
                            // regular escape, copy both
                            result.append(c); result.append(regex[i+1])
                            i += 2
                        }
                    }
                }

                // opening paren: check for flag group or scope
                c == '(' && i+1 < regex.length && regex[i+1] == '?' -> {
                    // scan forward to find the flag content
                    val flagStart = i + 2
                    var j = flagStart
                    // lookahead to end of group/scope/other, set j to that position
                    while (j < regex.length && regex[j] != ':' && regex[j] != ')' && regex[j] != '(') j++

                    // check if regex[i..j] forms valid flag scope/group
                    if (j < regex.length && (regex[j] == ':' || regex[j] == ')') && j > i+2
                        && regex.substring(i+2, j).all{ it in RegexFlags.validFlagCharacters || it == '-' }) {
                        // valid flag group/scope
                        if(regex[j] == ':') {
                            // flag group (?flags:...): parse flags and push scope
                            val flagToken = regex.substring(i, j+1) // e.g. "(?iu:"
                            val newFlags = currentFlags.merge(ParsedFlagExpression.fromFlagToken(flagToken))
                            scopeStack.addLast(currentFlags)
                            currentFlags = newFlags
                            result.append(regex.substring(i, j+1))
                            i = j + 1
                        } else {
                            // flag scope (?flags), update currentFlags
                            val flagToken = regex.substring(i, j+1) // e.g. "(?iu)"
                            currentFlags = currentFlags.merge(ParsedFlagExpression.fromFlagToken(flagToken))
                            result.append(regex.substring(i, j+1))
                            i = j + 1
                        }
                    } else {
                        // not a flag group/scope: push current flags unchanged
                        scopeStack.addLast(currentFlags)
                        result.append(c); i++
                    }
                }

                c == '(' -> {
                    scopeStack.addLast(currentFlags)
                    result.append(c); i++
                }

                c == ')' -> {
                    currentFlags = scopeStack.removeLastOrNull() ?: externalRegexFlags
                    result.append(c); i++
                }

                // comment (when COMMENTS flag is on)
                c == '#' && currentFlags.comments -> {
                    i++
                    // advance index until line terminator (eg: "#...\n") without copying
                    while (i < regex.length && !currentFlags.isLineTerminator(regex[i])) i++
                    // consume line terminator too:
                    if (i < regex.length) {
                        // \r\n is a 2-character line terminator
                        if (regex[i] == '\r' && i+1 < regex.length && regex[i+1] == '\n') i += 2
                        else i++
                    }
                }

                // whitespace, skip copying when comments flag is on
                c.isWhitespace() && currentFlags.comments -> i++

                // else copy
                else -> { result.append(c); i++ }
            }
        }
        return result.toString()
    }

    /**
     * Given a ECMA262 regex string, generate RegexGene for it.
     * Based on RegexEcma262.g4 file.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGeneForEcma262(regex: String): RegexGene {

        if(cacheEcma262.contains(regex)){
            return cacheEcma262[regex]!!.copy() as RegexGene
        }

        val stream = CharStreams.fromString(regex)
        val lexer = RegexEcma262Lexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = RegexEcma262Parser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GeneRegexEcma262Visitor().visit(pattern)

        val gene= res.genes.first() as RegexGene
        cacheEcma262[regex] = gene.copy() as RegexGene
        return gene
    }

    /**
     * Given a Postgres LIKE string constraint, generate RegexGene for it.
     * Based on PostgresLike.g4 file.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGeneForPostgresLike(regex: String): RegexGene {

        if(cachePostgresLike.contains(regex)){
            return cachePostgresLike[regex]!!.copy() as RegexGene
        }

        val stream = CharStreams.fromString(regex)
        val lexer = PostgresLikeLexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = PostgresLikeParser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GenePostgresLikeVisitor().visit(pattern)

        val gene= res.genes.first() as RegexGene
        cachePostgresLike[regex] = gene.copy() as RegexGene
        return gene
    }

    /**
     * Given a Postgres SIMILAR TO string constraint, generate RegexGene for it.
     * Based on PostgresSimilarTo.g4 file.
     *
     * This would throw an exception if regex is invalid, or if it
     * has features we do not support yet
     */
    fun createGeneForPostgresSimilarTo(regex: String): RegexGene {

        if(cachePostgresSimilarTo.contains(regex)){
            return cachePostgresSimilarTo[regex]!!.copy() as RegexGene
        }

        val stream = CharStreams.fromString(regex)
        val lexer = PostgresSimilarToLexer(stream)
        val tokenStream = prepareLexer(lexer)
        val parser = PostgresSimilarToParser(tokenStream)
        prepareParser(parser)

        val pattern = parser.pattern()

        val res = GenePostgresSimilarToVisitor().visit(pattern)

        val gene= res.genes.first() as RegexGene
        cachePostgresSimilarTo[regex] = gene.copy() as RegexGene
        return gene
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