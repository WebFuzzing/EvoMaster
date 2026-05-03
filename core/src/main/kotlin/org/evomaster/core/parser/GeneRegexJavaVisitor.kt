package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.utils.CharacterRange

private const val EOF_TOKEN = "<EOF>"
/**
 * Created by arcuri82 on 11-Sep-19.
 */
class GeneRegexJavaVisitor : RegexJavaBaseVisitor<VisitResult>(){

    /**
     * These are the Java regex syntax characters, all of these can be escaped to be treated as literals.
     */
    private val allowedSyntaxEscapes = "^$\\.*+?()[]{}|/-,:<>=!"

    /**
     * Tracks the flags active in the current lexical scope.
     * Updated when entering a flag group, restored on exit.
     */
    private var currentFlags = RegexFlags()

    /**
     * Parses a FLAG_GROUP_OPEN or FLAG_SCOPE_OPEN token text like "(?i:", "(?iu:", "(?-i:", "(?i-u:", "(?iu)", etc.
     * into a pair of (flagsToEnable, flagsToDisable).
     */
    private fun parseFlagToken(tokenText: String): Pair<RegexFlags, RegexFlags> {
        // strip "(?" from start and ":" (or ")") from end
        val inner = tokenText.drop(2).dropLast(1)

        val (enableStr, disableStr) = if ('-' in inner)
            inner.split('-', limit = 2).let { it[0] to it[1] }
        else Pair(inner, "")

        return Pair(RegexFlags.fromString(enableStr), RegexFlags.fromString(disableStr))
    }


    override fun visitPattern(ctx: RegexJavaParser.PatternContext): VisitResult {

        val res = ctx.disjunction().accept(this)

        val text = RegexUtils.getRegexExpByParserRuleContext(ctx)

        val disjList = DisjunctionListRxGene(res.genes.map { it as DisjunctionRxGene })

        // we remove the <EOF> token from end of the string to store as sourceRegex
        val gene = RegexGene(
            "regex",
            disjList,
            text.substring(0, text.length - EOF_TOKEN.length),
            RegexType.JVM
        )

        return VisitResult(gene)
    }

    override fun visitDisjunction(ctx: RegexJavaParser.DisjunctionContext): VisitResult {

        val altRes = ctx.alternative().accept(this)
        val assertionMatches = altRes.data as Pair<Boolean, Boolean>

        val matchStart = assertionMatches.first
        val matchEnd = assertionMatches.second

        val disj = DisjunctionRxGene("disj", altRes.genes.map { it  }, matchStart, matchEnd)

        val res = VisitResult(disj)

        if(ctx.disjunction() != null){
            val disjRes = ctx.disjunction().accept(this)
            res.genes.addAll(disjRes.genes)
        }

        return res
    }

    override fun visitAlternative(ctx: RegexJavaParser.AlternativeContext): VisitResult {

        val res = VisitResult()

        var caret = false
        var dollar = false

        for(i in 0 until ctx.term().size){

            val resTerm = ctx.term()[i].accept(this)
            val gene = resTerm.genes.firstOrNull()

            if(gene != null) {
                res.genes.add(gene)
            } else {

                val assertion = resTerm.data as String
                if(i==0 && assertion == "^"){
                    caret = true
                } else if(i==ctx.term().size-1 && assertion== "$"){
                    dollar = true
                } else {
                    /*
                        TODO in a regex, ^ and $ could be in any position, as representing
                        beginning and end of a line, and a regex could be multiline with
                        line terminator symbols
                     */
                    throw IllegalStateException("Cannot support $assertion at position $i")
                }
            }
        }

        res.data = Pair(caret, dollar)

        return res
    }

    override fun visitTerm(ctx: RegexJavaParser.TermContext): VisitResult {

        val res = VisitResult()

        if(ctx.assertion() != null){
            res.data = ctx.assertion().text
            return res
        }

        val resAtom = ctx.atom().accept(this)
        val atom = resAtom.genes.firstOrNull()
                ?: return res

        if(ctx.quantifier() != null){

            val limits = ctx.quantifier().accept(this).data as Pair<Int,Int>
            val q = QuantifierRxGene("q", atom, limits.first, limits.second)

            res.genes.add(q)

        } else {
            res.genes.add(atom)
        }

        return res
    }

    override fun visitQuantifier(ctx: RegexJavaParser.QuantifierContext): VisitResult {

        //TODO check how to handle "?" here

        return ctx.quantifierPrefix().accept(this)
    }

    override fun visitQuantifierPrefix(ctx: RegexJavaParser.QuantifierPrefixContext): VisitResult {

        val res = VisitResult()

        var min = 1
        var max = 1

        if(ctx.bracketQuantifier() == null){

            val symbol = ctx.text

            when(symbol){
                "*" -> {min=0; max= Int.MAX_VALUE}
                "+" -> {min=1; max= Int.MAX_VALUE}
                "?" -> {min=0; max=1}
                else -> throw IllegalArgumentException("Invalid quantifier symbol: $symbol")
            }
        } else {

            val q = ctx.bracketQuantifier()
            when {
                q.bracketQuantifierOnlyMin() != null -> {
                    min = q.bracketQuantifierOnlyMin().decimalDigits().text.toInt()
                    max = Int.MAX_VALUE
                }
                q.bracketQuantifierSingle() != null -> {
                    min = q.bracketQuantifierSingle().decimalDigits().text.toInt()
                    max = min
                }
                q.bracketQuantifierRange() != null -> {
                    val range = q.bracketQuantifierRange()
                    min = range.decimalDigits()[0].text.toInt()
                    max = range.decimalDigits()[1].text.toInt()
                }
                else -> throw IllegalArgumentException("Invalid quantifier: ${ctx.text}")
            }
        }

        res.data = Pair(min,max)

        return res
    }

    override fun visitAtom(ctx: RegexJavaParser.AtomContext): VisitResult {

        // flag group: (?i:disjunction) (?iu:disjunction) (?-i:disjunction) etc.
        if (ctx.FLAG_GROUP_OPEN() != null) {
            val (toEnable, toDisable) = parseFlagToken(ctx.FLAG_GROUP_OPEN().text)

            val previous = currentFlags
            val merged = previous.merge(toEnable, toDisable)

            merged.validate()

            currentFlags = merged

            val res = ctx.disjunction().accept(this)

            currentFlags = previous

            val disjList = DisjunctionListRxGene(res.genes.map { it as DisjunctionRxGene })

            //TODO tmp hack until full handling of ^$. Assume full match when nested disjunctions
            for (gene in disjList.disjunctions) {
                gene.extraPrefix = false
                gene.extraPostfix = false
                gene.matchStart = true
                gene.matchEnd = true
            }

            return VisitResult(disjList)
        }

        if(ctx.quote() != null){

            val block = ctx.quote().quoteBlock().quoteChar().map { it.text }
                    .joinToString("")

            val name = if(block.isBlank()) "blankBlock" else block

            val gene = PatternCharacterBlockGene(name, block, currentFlags)

            return VisitResult(gene)
        }

        if(! ctx.patternCharacter().isEmpty()){
            val block = ctx.patternCharacter().map { it.text }
                    .joinToString("")

            val gene = PatternCharacterBlockGene("block", block, currentFlags)

            return VisitResult(gene)
        }

        if(ctx.atomEscape() != null) {
            return ctx.atomEscape().accept(this)
        }

        if(ctx.disjunction() != null){

            val res = ctx.disjunction().accept(this)

            val disjList = DisjunctionListRxGene(res.genes.map { it as DisjunctionRxGene })

            //TODO tmp hack until full handling of ^$. Assume full match when nested disjunctions
            for(gene in disjList.disjunctions){
                gene.extraPrefix = false
                gene.extraPostfix = false
                gene.matchStart = true
                gene.matchEnd = true
            }

            return VisitResult(disjList)
        }

        if(ctx.DOT() != null){
            return VisitResult(AnyCharacterRxGene())
        }

        if(ctx.characterClass() != null){
            return ctx.characterClass().accept(this)
        }

        throw IllegalStateException("No valid atom resolver for: ${ctx.text}")
    }


    override fun visitCharacterClass(ctx: RegexJavaParser.CharacterClassContext): VisitResult {

        val negated = ctx.CARET() != null

        val ranges = ctx.classRanges().accept(this).data as List<CharacterRange>

        val gene = CharacterRangeRxGene(negated, ranges, currentFlags)

        return VisitResult(gene)
    }

    override fun visitClassRanges(ctx: RegexJavaParser.ClassRangesContext): VisitResult {

        val res = VisitResult()
        val list = mutableListOf<CharacterRange>()

        if(ctx.nonemptyClassRanges() != null){
            val ranges = ctx.nonemptyClassRanges().accept(this).data as List<CharacterRange>
            list.addAll(ranges)
        }

        res.data = list

        return res
    }

    override fun visitNonemptyClassRanges(ctx: RegexJavaParser.NonemptyClassRangesContext): VisitResult {

        val list = mutableListOf<CharacterRange>()

        if (ctx.classAtom()[0]?.classAtomNoDash()?.classEscape() != null){
            if (ctx.classAtom().size == 2) throw IllegalArgumentException("Not implemented yet")
            val rec = ctx.classAtom()[0].accept(this).data as List<CharacterRange>
            list.addAll(rec)
        } else {
            val startText = ctx.classAtom()[0].text
            assert(startText.length == 1 || startText.length == 2) // single chars or \+ and \. escaped chars

            val start: Char
            val end: Char

            if (startText.length == 1) {
                start = startText[0]
                end = if (ctx.classAtom().size == 2) {
                    ctx.classAtom()[1].text[0]
                } else {
                    //single char, not an actual range
                    start
                }
            } else {
                // This case handles the escaped syntax characters, like "\." and "\+", etc. cases
                // where '.' and '+', etc. should be treated as regular chars
                assert(startText[0] == '\\' && startText[1] in allowedSyntaxEscapes)
                start = startText[1]
                end = start
            }

            list.add(CharacterRange(start, end))
        }

        if(ctx.nonemptyClassRangesNoDash() != null){
            val ranges = ctx.nonemptyClassRangesNoDash().accept(this).data as List<CharacterRange>
            list.addAll(ranges)
        }

        if(ctx.classRanges() != null){
            val ranges = ctx.classRanges().accept(this).data as List<CharacterRange>
            list.addAll(ranges)
        }

        val res = VisitResult()
        res.data = list

        return res
    }


    override fun visitNonemptyClassRangesNoDash(ctx: RegexJavaParser.NonemptyClassRangesNoDashContext): VisitResult {

        val list = mutableListOf<CharacterRange>()

        if(ctx.MINUS() != null){

            val start = ctx.classAtomNoDash().text[0]
            val end = ctx.classAtom().text[0]
            list.add(CharacterRange(start, end))

        } else {

            if (ctx.classAtom()?.classAtomNoDash()?.classEscape() != null || ctx.classAtomNoDash()?.classEscape() != null){
                val rec = (ctx.classAtom() ?: ctx.classAtomNoDash()).accept(this).data as List<CharacterRange>
                list.addAll(rec)
            } else {
                val text = (ctx.classAtom() ?: ctx.classAtomNoDash()).text
                if(text.length==1) {
                    list.add(CharacterRange(text[0], text[0]))
                }
                else {
                    list.add(CharacterRange(text[1], text[1]))
                }
            }
        }

        if(ctx.nonemptyClassRangesNoDash() != null){
            val ranges = ctx.nonemptyClassRangesNoDash().accept(this).data as List<CharacterRange>
            list.addAll(ranges)
        }

        if(ctx.classRanges() != null){
            val ranges = ctx.classRanges().accept(this).data as List<CharacterRange>
            list.addAll(ranges)
        }

        val res = VisitResult()
        res.data = list

        return res
    }

    override fun visitClassEscape(ctx: RegexJavaParser.ClassEscapeContext): VisitResult {

        val res = VisitResult()
        res.data = if(ctx.atomEscape() != null) {
            when (val rec = ctx.atomEscape().accept(this).genes[0]) {
                is CharacterClassEscapeRxGene -> {
                    rec.multiCharRange.ranges
                }

                is PatternCharacterBlockGene -> {
                    if (rec.stringBlock.length > 1) {
                        throw IllegalArgumentException("CharClass element cannot be strings")
                    }
                    else listOf(CharacterRange(rec.stringBlock[0], rec.stringBlock[0]))
                }

                else -> throw IllegalArgumentException("Unexpected CharClass content")
            }
        } else {
            throw IllegalArgumentException("Not implemented yet")
        }
        return res
    }

    override fun visitAtomEscape(ctx: RegexJavaParser.AtomEscapeContext): VisitResult {

        val txt = ctx.text

        return VisitResult(when (txt[1]) {
            '0' -> {
                val octalValue = txt.substring(2).toInt(8)
                PatternCharacterBlockGene(
                        txt,
                        String(Character.toChars(octalValue)),
                        currentFlags
                )
            }
            'c' -> {
                val controlLetterValue = if (txt[2].isLowerCase()){
                    txt[2].uppercaseChar().code.xor(0x60)
                } else {
                    txt[2].code.xor(0x40)
                }
                PatternCharacterBlockGene(txt, controlLetterValue.toChar().toString(), currentFlags)
            }
            in "aefnrt" -> {
                val escape = when (txt[1]) {
                    'a' -> "\u0007"
                    'e' -> "\u001B"
                    'f' -> "\u000C"
                    'n' -> "\u000A"
                    'r' -> "\u000D"
                    else -> "\u0009"
                }
                PatternCharacterBlockGene(txt, escape, currentFlags)
            }
            in "xu" -> {
                val hexValue = if (txt[1] == 'x' && txt.length > 4 && txt[2] == '{' && txt[txt.length - 1] == '}') {
                    txt.substring(3, txt.length - 1).toInt(16)
                } else {
                    txt.substring(2).toInt(16)
                }
                if(hexValue !in Character.MIN_CODE_POINT..Character.MAX_CODE_POINT){
                    throw IllegalArgumentException("Hexadecimal escape out of range: ${ctx.text}")
                }
                PatternCharacterBlockGene(
                        txt,
                        String(Character.toChars(hexValue)),
                        currentFlags
                )
            }
            in allowedSyntaxEscapes -> PatternCharacterBlockGene(txt, txt.substring(1), currentFlags)
            else -> CharacterClassEscapeRxGene(txt.substring(1), currentFlags)
        })
    }
}
