package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.*

/**
 * Parser Visitor based on the RegexEcma262.g4 grammar file
 */
class GeneRegexEcma262Visitor : RegexEcma262BaseVisitor<VisitResult>(){


    override fun visitPattern(ctx: RegexEcma262Parser.PatternContext): VisitResult {

        val res = ctx.disjunction().accept(this)

        val disjList = DisjunctionListRxGene(res.genes.map { it as DisjunctionRxGene })

        val gene = RegexGene("regex", disjList)

        return VisitResult(gene)
    }

    override fun visitDisjunction(ctx: RegexEcma262Parser.DisjunctionContext): VisitResult {

        val altRes = ctx.alternative().accept(this)
        val assertionMatches = altRes.data as Pair<Boolean, Boolean>

        val matchStart = assertionMatches.first
        val matchEnd = assertionMatches.second

        val disj = DisjunctionRxGene("disj", altRes.genes.map { it as RxTerm }, matchStart, matchEnd)

        val res = VisitResult(disj)

        if(ctx.disjunction() != null){
            val disjRes = ctx.disjunction().accept(this)
            res.genes.addAll(disjRes.genes)
        }

        return res
    }

    override fun visitAlternative(ctx: RegexEcma262Parser.AlternativeContext): VisitResult {

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

    override fun visitTerm(ctx: RegexEcma262Parser.TermContext): VisitResult {

        val res = VisitResult()

        if(ctx.assertion() != null){
            res.data = ctx.assertion().text
            return res
        }

        val resAtom = ctx.atom().accept(this)
        val atom = resAtom.genes.firstOrNull() as RxAtom?
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

    override fun visitQuantifier(ctx: RegexEcma262Parser.QuantifierContext): VisitResult {

        //TODO check how to handle "?" here

        return ctx.quantifierPrefix().accept(this)
    }

    override fun visitQuantifierPrefix(ctx: RegexEcma262Parser.QuantifierPrefixContext): VisitResult {

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

    override fun visitAtom(ctx: RegexEcma262Parser.AtomContext): VisitResult {

        if(! ctx.patternCharacter().isEmpty()){
            val block = ctx.patternCharacter().map { it.text }
                    .joinToString("")

            val gene = PatternCharacterBlock("block", block)

            return VisitResult(gene)
        }

        if(ctx.AtomEscape() != null){
            val char = ctx.AtomEscape().text[1].toString()
            return VisitResult(CharacterClassEscapeRxGene(char))
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


    override fun visitCharacterClass(ctx: RegexEcma262Parser.CharacterClassContext): VisitResult {

        val negated = ctx.CARET() != null

        val ranges = ctx.classRanges().accept(this).data as List<Pair<Char,Char>>

        val gene = CharacterRangeRxGene(negated, ranges)

        return VisitResult(gene)
    }

    override fun visitClassRanges(ctx: RegexEcma262Parser.ClassRangesContext): VisitResult {

        val res = VisitResult()
        val list = mutableListOf<Pair<Char,Char>>()

        if(ctx.nonemptyClassRanges() != null){
            val ranges = ctx.nonemptyClassRanges().accept(this).data as List<Pair<Char,Char>>
            list.addAll(ranges)
        }

        res.data = list

        return res
    }

    override fun visitNonemptyClassRanges(ctx: RegexEcma262Parser.NonemptyClassRangesContext): VisitResult {

        val list = mutableListOf<Pair<Char,Char>>()

        val startText = ctx.classAtom()[0].text
        assert(startText.length == 1) // single chars
        val start : Char = startText[0]

        val end = if(ctx.classAtom().size == 2){
            ctx.classAtom()[1].text[0]
        } else {
            //single char, not an actual range
            start
        }

        list.add(Pair(start, end))

        if(ctx.nonemptyClassRangesNoDash() != null){
            val ranges = ctx.nonemptyClassRangesNoDash().accept(this).data as List<Pair<Char,Char>>
            list.addAll(ranges)
        }

        if(ctx.classRanges() != null){
            val ranges = ctx.classRanges().accept(this).data as List<Pair<Char,Char>>
            list.addAll(ranges)
        }

        val res = VisitResult()
        res.data = list

        return res
    }


    override fun visitNonemptyClassRangesNoDash(ctx: RegexEcma262Parser.NonemptyClassRangesNoDashContext): VisitResult {

        val list = mutableListOf<Pair<Char,Char>>()

        if(ctx.MINUS() != null){

            val start = ctx.classAtomNoDash().text[0]
            val end = ctx.classAtom().text[0]
            list.add(Pair(start, end))

        } else {

            val char = (ctx.classAtom() ?: ctx.classAtomNoDash()).text[0]
            list.add(Pair(char, char))
        }

        if(ctx.nonemptyClassRangesNoDash() != null){
            val ranges = ctx.nonemptyClassRangesNoDash().accept(this).data as List<Pair<Char,Char>>
            list.addAll(ranges)
        }

        if(ctx.classRanges() != null){
            val ranges = ctx.classRanges().accept(this).data as List<Pair<Char,Char>>
            list.addAll(ranges)
        }

        val res = VisitResult()
        res.data = list

        return res
    }

}