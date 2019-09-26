package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.*

/**
 * Created by arcuri82 on 12-Jun-19.
 */
class GenePostgresSimilarToVisitor : PostgresSimilarToBaseVisitor<VisitResult>() {

    /*
        WARNING: lot of code here is similar/adapted from ECMA262 visitor.
        But, as the parser objects are different, it does not seem simple to reuse
        the code without avoiding copy&paste&adapt :(
     */

    override fun visitPattern(ctx: PostgresSimilarToParser.PatternContext): VisitResult {

        val res = ctx.disjunction().accept(this)

        val disjList = DisjunctionListRxGene(res.genes.map { it as DisjunctionRxGene })

        val gene = RegexGene("regex", disjList)

        return VisitResult(gene)
    }

    override fun visitDisjunction(ctx: PostgresSimilarToParser.DisjunctionContext): VisitResult {

        val altRes = ctx.alternative().accept(this)

        val disj = DisjunctionRxGene("disj", altRes.genes.map { it as RxTerm }, true, true)

        val res = VisitResult(disj)

        if(ctx.disjunction() != null){
            val disjRes = ctx.disjunction().accept(this)
            res.genes.addAll(disjRes.genes)
        }

        return res
    }

    override fun visitAlternative(ctx: PostgresSimilarToParser.AlternativeContext): VisitResult {

        val res = VisitResult()


        for(i in 0 until ctx.term().size){

            val resTerm = ctx.term()[i].accept(this)
            val gene = resTerm.genes.firstOrNull()

            if(gene != null) {
                res.genes.add(gene)
            }
        }

        return res
    }

    override fun visitTerm(ctx: PostgresSimilarToParser.TermContext): VisitResult {

        val res = VisitResult()

        val resAtom = ctx.atom().accept(this)

        val atom = resAtom.genes.firstOrNull()
                ?: return res

        if(ctx.quantifier() != null){

            val limits = ctx.quantifier().accept(this).data as Pair<Int,Int>
            val q = QuantifierRxGene("q", atom as RxAtom, limits.first, limits.second)

            res.genes.add(q)

        } else {
            res.genes.add(atom)
        }

        return res
    }


    override fun visitQuantifier(ctx: PostgresSimilarToParser.QuantifierContext): VisitResult {

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

    override fun visitAtom(ctx: PostgresSimilarToParser.AtomContext): VisitResult {

        if(! ctx.patternCharacter().isEmpty()){
            val block = ctx.patternCharacter().map { it.text }
                    .joinToString("")

            val gene = PatternCharacterBlock("block", block)

            return VisitResult(gene)
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

        if(ctx.UNDERSCORE() != null){
            return VisitResult(AnyCharacterRxGene())
        }

        if(ctx.PERCENT() != null){
            return VisitResult(QuantifierRxGene("q", AnyCharacterRxGene(), 0 , Int.MAX_VALUE))
        }

        if(ctx.characterClass() != null){
            return ctx.characterClass().accept(this)
        }

        throw IllegalStateException("No valid atom resolver for: ${ctx.text}")
    }


    override fun visitCharacterClass(ctx: PostgresSimilarToParser.CharacterClassContext): VisitResult {

        val negated = ctx.CARET() != null

        val ranges = ctx.classRanges().accept(this).data as List<Pair<Char,Char>>

        val gene = CharacterRangeRxGene(negated, ranges)

        return VisitResult(gene)
    }

    override fun visitClassRanges(ctx: PostgresSimilarToParser.ClassRangesContext): VisitResult {

        val res = VisitResult()
        val list = mutableListOf<Pair<Char,Char>>()

        if(ctx.nonemptyClassRanges() != null){
            val ranges = ctx.nonemptyClassRanges().accept(this).data as List<Pair<Char,Char>>
            list.addAll(ranges)
        }

        res.data = list

        return res
    }

    override fun visitNonemptyClassRanges(ctx: PostgresSimilarToParser.NonemptyClassRangesContext): VisitResult {

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


    override fun visitNonemptyClassRangesNoDash(ctx: PostgresSimilarToParser.NonemptyClassRangesNoDashContext): VisitResult {

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