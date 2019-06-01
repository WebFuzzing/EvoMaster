package org.evomaster.core.parser.visitor

import org.evomaster.core.parser.RegexEcma262BaseVisitor
import org.evomaster.core.parser.RegexEcma262Parser
import org.evomaster.core.search.gene.regex.*


class Ecma262Visitor : RegexEcma262BaseVisitor<VisitResult>(){


    override fun visitPattern(ctx: RegexEcma262Parser.PatternContext): VisitResult {

        val res = ctx.disjunction().accept(this)

        val disjList = DisjunctionListRxGene(res.genes.map { it as DisjunctionRxGene })

        val gene = RegexGene("regex", disjList)

        return VisitResult(gene)
    }

    override fun visitDisjunction(ctx: RegexEcma262Parser.DisjunctionContext): VisitResult {

        val altRes = ctx.alternative().accept(this)

        val disj = DisjunctionRxGene("disj", altRes.genes.map { it as RxTerm })

        val res = VisitResult(disj)

        if(ctx.disjunction() != null){
            val disjRes = ctx.disjunction().accept(this)
            res.genes.addAll(disjRes.genes)
        }

        return res
    }

    override fun visitAlternative(ctx: RegexEcma262Parser.AlternativeContext): VisitResult {

        val res = VisitResult()

        ctx.term().forEach {
            val resTerm = it.accept(this)
            val gene = resTerm.genes.firstOrNull()
            if(gene != null) {
                res.genes.add(gene)
            }
        }

        return res
    }

    override fun visitTerm(ctx: RegexEcma262Parser.TermContext): VisitResult {

        val res = VisitResult()

        if(ctx.assertion() != null){
            //TODO
            throw IllegalStateException("regex assertion not supported yet")
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

        //TODO properly all cases

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
                    min = q.bracketQuantifierOnlyMin().DecimalDigits().text.toInt()
                    max = Int.MAX_VALUE
                }
                q.bracketQuantifierSingle() != null -> {
                    min = q.bracketQuantifierSingle().DecimalDigits().text.toInt()
                    max = min
                }
                q.bracketQuantifierRange() != null -> {
                    val range = q.bracketQuantifierRange()
                    min = range.DecimalDigits()[0].text.toInt()
                    max = range.DecimalDigits()[1].text.toInt()
                }
                else -> throw IllegalArgumentException("Invalid quantifier: ${ctx.text}")
            }
        }

        res.data = Pair(min,max)

        return res
    }

    override fun visitAtom(ctx: RegexEcma262Parser.AtomContext): VisitResult {

        if(! ctx.PatternCharacter().isEmpty()){
            val block = ctx.PatternCharacter().map { it.text }
                    .joinToString("")

            val gene = PatternCharacterBlock(block, block)

            return VisitResult(gene)
        }

        //TODO check this one if really needed in the parser
        if(ctx.DecimalDigits() != null){
            val block = ctx.DecimalDigits().text
            return VisitResult(PatternCharacterBlock(block, block))
        }

        if(ctx.AtomEscape() != null){
            val char = ctx.AtomEscape().text[1].toString()
            return VisitResult(CharacterClassEscapeRxGene(char))
        }

        //TODO "."

        throw IllegalStateException("No valid atom resolver for: ${ctx.text}")
    }


//    override fun visitAssertion(ctx: RegexEcma262Parser.AssertionContext): VisitResult {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

}