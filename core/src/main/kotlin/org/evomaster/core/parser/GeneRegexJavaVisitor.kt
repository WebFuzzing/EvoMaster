package org.evomaster.core.parser

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.utils.CharacterRange
import org.evomaster.core.utils.MultiCharacterRange
import org.evomaster.core.utils.ParsedFlagExpression
import org.evomaster.core.utils.RegexFlags

private const val EOF_TOKEN = "<EOF>"
/**
 * Created by arcuri82 on 11-Sep-19.
 */
class GeneRegexJavaVisitor(val externalRegexFlags: RegexFlags = RegexFlags()) : RegexJavaBaseVisitor<VisitResult>(){

    private val hexEscapePrefixes = setOf('x', 'u')

    /**
     * Mappings of various escapes to their matching characters.
     */
    private val escapeMap = mapOf(
        'a' to "\u0007",
        'e' to "\u001B",
        'f' to "\u000C",
        'n' to "\u000A",
        'r' to "\u000D",
        't' to "\u0009"
    )

    /**
     * None of these can be escaped to be treated as literals. Some may be part of legal escape sequences.
     */
    private val notIdentityEscapes = ('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()

    /**
     * Capture groups in order of appearance (1-based index -> list index 0).
     * Populated as the tree is walked. A backreference is only valid if it
     * appears after the group it references, which Java regex requires anyway.
     * The value is nullable to represent a captured group that is unsatisfiable,
     * for example when the group contains an empty character class like `([a&&b])`.
     * In that case the map holds null instead of a DisjunctionListRxGene.
     * @see buildDisjunctionList
     */
    private val captureGroups = mutableListOf<DisjunctionListRxGene?>()

    /**
     * Same as [captureGroups] but for named backreferences, which can be accessed
     * with their name or number.
     * The value is nullable to represent a captured group that is unsatisfiable,
     * for example when the group contains an empty character class like `([a&&b])`.
     * In that case the map holds null instead of a DisjunctionListRxGene.
     * @see buildDisjunctionList
     */
    private val namedCaptureGroups = mutableMapOf<String, DisjunctionListRxGene?>()

    /**
     * Tracks the flags active in the current lexical scope.
     * Updated when entering a flag group, restored on exit.
     * Initialized from [externalRegexFlags].
     */
    private var currentFlags = externalRegexFlags

    /**
     * Builds DisjunctionListRxGenes from a disjunction context, returns null if disjunction is unsatisfiable.
     */
    private fun buildDisjunctionList(ctx: RegexJavaParser.DisjunctionContext): DisjunctionListRxGene? {
        val res = ctx.accept(this)
        val validDisjunctions = res.genes.map { it as DisjunctionRxGene }

        val satisfiableDisjunctions = validDisjunctions.filter{ !it.isUnsatisfiable() }

        if(satisfiableDisjunctions.isEmpty()){
            // As DisjunctionListRxGene extends CompositeFixedGene, its disjunctions list cannot be empty.
            // In this case we return null to represent an unsatisfiable DisjunctionListRxGene.
            return null
        }

        val disjList = DisjunctionListRxGene(satisfiableDisjunctions)

        //TODO tmp hack until full handling of ^$. Assume full match when nested disjunctions
        for (gene in disjList.disjunctions) {
            gene.extraPrefix = false
            gene.extraPostfix = false
            gene.matchStart = true
            gene.matchEnd = true
        }
        return disjList
    }

    override fun visitPattern(ctx: RegexJavaParser.PatternContext): VisitResult {

        val res = ctx.disjunction().accept(this)

        val text = RegexUtils.getRegexExpByParserRuleContext(ctx)

        val satisfiableDisjunctions = res.genes
            .map { it as DisjunctionRxGene }
            .filter{ !it.isUnsatisfiable() }

        if (satisfiableDisjunctions.isEmpty()) {
            throw IllegalStateException("Regex is unsatisfiable.")
        }

        val disjList = DisjunctionListRxGene(satisfiableDisjunctions)

        // we remove the <EOF> token from end of the string to store as sourceRegex
        val gene = RegexGene(
            "regex",
            disjList,
            text.substring(0, text.length - EOF_TOKEN.length),
            RegexType.JVM,
            externalRegexFlags = externalRegexFlags
        )

        return VisitResult(gene)
    }

    override fun visitDisjunction(ctx: RegexJavaParser.DisjunctionContext): VisitResult {

        val altRes = ctx.alternative().accept(this)
        val assertionMatches = altRes.data as Pair<Boolean, Boolean>

        val matchStart = assertionMatches.first
        val matchEnd = assertionMatches.second

        val res = VisitResult()

        // add disjunction if it has genes, OR if the alternative was purely assertions (^$) or flag scopes
        // in that case altRes.genes is empty but the alternative is valid (matches "")
        val hasOnlyAssertionsOrFlagScopes = ctx.alternative().term().isNotEmpty() &&
                ctx.alternative().term().all { it.assertion() != null || it.FLAG_SCOPE_OPEN() != null }

        if (altRes.genes.isNotEmpty() || hasOnlyAssertionsOrFlagScopes || ctx.alternative().term().isEmpty()) {
            val disj = DisjunctionRxGene("disj", altRes.genes.map { it }, matchStart, matchEnd)

            res.genes.add(disj)
        }
        // else: had non-assertion terms but all produced nothing (empty char class etc.), skip

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

            val term = ctx.term()[i]

            if (term.FLAG_SCOPE_OPEN() != null) {
                val previous = currentFlags

                val merged = currentFlags.merge(
                    ParsedFlagExpression.fromFlagToken(term.FLAG_SCOPE_OPEN().text)
                )

                merged.validate()

                currentFlags = merged

                // Visit all remaining terms under the new flags. Same as what
                // visitAtom does for the colon form, just consuming terms instead
                // of a disjunction subtree.
                val remainingGenes = mutableListOf<Gene>()
                for (j in i + 1 until ctx.term().size) {
                    val resTerm = ctx.term()[j].accept(this)

                    // this condition isolates the back ref case, preserving original behavior otherwise.
                    if (ctx.term()[j].atom()?.atomEscape()?.BackReference() != null){
                        // if term is a BackReference we addAll genes from result as there may be more than one if digits are dropped
                        remainingGenes.addAll(resTerm.genes)
                    } else {
                        // term is not a back ref, hence there is at most one gene in its visit result.
                        resTerm.genes.firstOrNull()?.let { remainingGenes.add(it) }
                    }
                }

                currentFlags = previous

                res.genes.addAll(remainingGenes)
                break // remaining terms already consumed
            }

            val resTerm = ctx.term()[i].accept(this)
            val gene = resTerm.genes.firstOrNull()

            // this condition isolates the back ref case, preserving original behavior otherwise.
            if (ctx.term()[i].atom()?.atomEscape()?.BackReference() != null){
                // if term is a BackReference we addAll genes from result as there may be more than one if digits are dropped
                res.genes.addAll(resTerm.genes)
            } else if (gene != null) {
                // term is not a back ref: we use the default behavior, term results may only have 0-1 genes
                // if there is a gene, we add it to result
                res.genes.add(gene)
            } else if (resTerm.data is String) {

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
            } else {
                // unsatisfiable term, return with no genes
                return VisitResult(data=Pair(false, false))
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

        if(ctx.quantifier() != null){

            val limits = ctx.quantifier().accept(this).data as Pair<Int,Int>

            // if quantified atom is unsatisfiable we must then check the limits
            if (atom == null ||
                ((atom as? RxTerm)?.isUnsatisfiable() == true) && resAtom.genes.size == 1) {
                return if (limits.first == 0) {
                    // if 0 appearances is allowed then the regex is satisfiable only with empty string
                    VisitResult(PatternCharacterBlockGene("0_QuantifierOnEmptyRegex", ""))
                } else {
                    // if not then unsatisfiable, return with no genes
                    res
                }
            }

            // if atom is not a back ref then we use the default behavior, results may only have one gene
            var template: Gene = atom

            // this condition isolates the back ref case, preserving original behavior otherwise.
            if(ctx.atom()?.atomEscape()?.BackReference() != null){
                // this is done so that visits that result in multiple genes (like a backref that interprets some
                // digits literally) work as expected, only applying quantifier to last gene

                // add all genes to result, except for last gene
                res.genes.addAll(resAtom.genes.dropLast(1))

                // the last gene gets wrapped with the quantifier gene, then that gets added to result
                template = resAtom.genes.last()
            }

            val q = QuantifierRxGene("q", template, limits.first, limits.second)

            res.genes.add(q)

        } else {
            // this condition isolates the back ref case, preserving original behavior otherwise.
            if (ctx.atom()?.atomEscape()?.BackReference() != null){
                // if atom is a BackReference we addAll genes from result as there may be more than one if digits are dropped
                res.genes.addAll(resAtom.genes)
            } else if (atom != null) {
                // if atom is not a back ref we fall back to the default behavior, results only have one gene
                res.genes.add(atom)
            }
            // else atom is unsatisfiable, return no genes
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
            val previous = currentFlags

            val merged = currentFlags.merge(
                ParsedFlagExpression.fromFlagToken(ctx.FLAG_GROUP_OPEN().text)
            )

            merged.validate()

            currentFlags = merged

            val disjList = buildDisjunctionList(ctx.disjunction())

            currentFlags = previous

            return if (disjList != null) {
                VisitResult(disjList)
            } else {
                // unsatisfiable, return with no genes.
                VisitResult()
            }
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

            // to correctly handle group nesting order, we must record group index before visiting
            val groupIndex = captureGroups.size
            captureGroups.add(null) // add placeholder for the gene

            val disjList = buildDisjunctionList(ctx.disjunction())

            val isCapturingGroup = !ctx.text.startsWith("(?:")
            val isNamedCaptureGroup = ctx.NAMED_CAPTURE_GROUP_OPEN() != null

            if (isCapturingGroup) {
                captureGroups[groupIndex] = disjList
            }
            if (isNamedCaptureGroup) {
                val name = ctx.NAMED_CAPTURE_GROUP_OPEN().text.drop(3).dropLast(1) // strip "(?<" and ")"
                if (namedCaptureGroups.containsKey(name)) {
                    throw IllegalStateException("Duplicate capture group name: '$name'")
                }
                namedCaptureGroups[name] = disjList
            }

            return if (disjList != null) {
                VisitResult(disjList)
            } else {
                // unsatisfiable, return with no genes.
                VisitResult()
            }
        }

        if(ctx.DOT() != null){
            return VisitResult(AnyCharacterRxGene(currentFlags))
        }

        if(ctx.characterClass() != null){
            return ctx.characterClass().accept(this)
        }

        throw IllegalStateException("No valid atom resolver for: ${ctx.text}")
    }


    override fun visitCharacterClass(ctx: RegexJavaParser.CharacterClassContext): VisitResult {

        val negated = ctx.CARET() != null

        val innerMultiCharRanges = ctx.classContents().accept(this).data as MultiCharacterRange

        val multiCharRanges = MultiCharacterRange(negated, innerMultiCharRanges)

        return if (ctx.parent is RegexJavaParser.AtomContext){
            // top level character class, create gene
            VisitResult(CharacterRangeRxGene(multiCharRanges, currentFlags))
        } else {
            // nested char class, set MultiCharacterRange as data
            VisitResult(data = multiCharRanges)
        }
    }

    override fun visitClassContents(ctx: RegexJavaParser.ClassContentsContext): VisitResult {

        // intersect the unions of ranges
        val mcr = ctx.classUnion()
            .map { it.accept(this).data as MultiCharacterRange }
            .reduce { acc, item -> MultiCharacterRange.intersect(acc, item) }

        return VisitResult(data=mcr)
    }

    override fun visitClassUnion(ctx: RegexJavaParser.ClassUnionContext): VisitResult {

        return if (ctx.characterClass().isNotEmpty()) {
            // union of char classes
            val mcr = ctx.characterClass()
                .map { it.accept(this).data as MultiCharacterRange }
                .reduce { acc, item -> MultiCharacterRange.union(acc, item) }

            VisitResult(data=mcr)
        } else {
            // single classRanges
            val ranges = ctx.classRanges().accept(this).data as List<CharacterRange>

            VisitResult(data=MultiCharacterRange(false, ranges))
        }
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
        } else if (
                ctx.classAtom()[0]?.classAtomNoDash()?.FLAG_SCOPE_OPEN() != null
                || ctx.classAtom()[0]?.classAtomNoDash()?.FLAG_GROUP_OPEN() != null
                || ctx.classAtom()[0]?.classAtomNoDash()?.NAMED_CAPTURE_GROUP_OPEN() != null
            ) {
            // these should be interpreted literally within a charclass.
            val ranges = ctx.text.map { ch -> CharacterRange(ch, ch) }
            list.addAll(ranges)
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
                assert(startText[0] == '\\' && startText[1] !in notIdentityEscapes)
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
        res.data = if(ctx.atomEscape() != null &&
            (ctx.atomEscape().BackReference() != null || ctx.atomEscape().NamedBackReference() != null)
            ) {
            // In Java using backrefs or named backrefs is illegal within char classes. (i.e.: [\1\k<name>])
            throw IllegalArgumentException("Illegal/unsupported escape sequence")
        } else if (ctx.atomEscape() != null) {
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

        // unnamed backreference \N (N number)
        if (ctx.BackReference() != null) {
            val allDigits = txt.drop(1)
            val maxDigits = captureGroups.size.toString().length

            // In Java, multi-digit back references interprets trailing digits literally, see more:
            // https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#groupname:~:text=the%20parser%20will-,drop%20digits,-until%20the%20number
            val backRefDigitCount = when {
                maxDigits > allDigits.length -> allDigits.length
                allDigits.take(maxDigits).toInt() <= captureGroups.size -> maxDigits
                maxDigits > 1 -> maxDigits - 1
                else -> 1
            }

            val n = allDigits.take(backRefDigitCount).toInt()

            val result = VisitResult(BackReferenceRxGene(n, captureGroups.getOrNull(n - 1)))

            val remainingChars = allDigits.drop(backRefDigitCount)

            for (char in remainingChars) {
                // we add the remaining digits as pattern genes to result as these should be interpreted literally
                result.genes.add(PatternCharacterBlockGene(char.toString(), char.toString()))
            }

            return result
        }

        // named backreference \k<name>
        if (ctx.NamedBackReference() != null) {
            // strip "\k<" and ">"
            val name = txt.drop(3).dropLast(1)
            if(name !in namedCaptureGroups){
                throw IllegalStateException("Named backreference \\k<$name> refers to unknown group '$name'")
            }
            val group = namedCaptureGroups[name]
            val groupIndex = captureGroups.indexOf(group) + 1  // 1-based, for the gene name
            return VisitResult(BackReferenceRxGene(groupIndex, group))
        }

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
            in escapeMap -> {
                val escape = escapeMap[txt[1]]!!
                PatternCharacterBlockGene(txt, escape, currentFlags)
            }
            in hexEscapePrefixes -> {
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
            !in notIdentityEscapes -> PatternCharacterBlockGene(txt, txt.substring(1), currentFlags)
            else -> CharacterClassEscapeRxGene(txt.substring(1), currentFlags)
        })
    }
}
