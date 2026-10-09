package org.evomaster.core.parser

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.utils.CharacterRange

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
        val text = RegexUtils.getRegexExpByParserRuleContext(ctx)

        val satisfiableDisjunctions = res.genes
            .map { it as DisjunctionRxGene }
            .filter { !it.isUnsatisfiable() }

        if (satisfiableDisjunctions.isEmpty()) {
            throw IllegalStateException("Regex is unsatisfiable.")
        }

        val disjList = DisjunctionListRxGene(satisfiableDisjunctions)

        val gene = RegexGene(
            "regex",
            disjList,
            text,
            RegexType.POSTGRES_SIMILAR_TO
        )

        return VisitResult(gene)
    }

    override fun visitDisjunction(ctx: PostgresSimilarToParser.DisjunctionContext): VisitResult {

        val res = VisitResult()
        val altRes = ctx.alternative().accept(this)

        // if altRes genes are empty and there were terms on the alternative then it was unsatisfiable, skip
        val isSatisfiable = altRes.genes.isNotEmpty() || ctx.alternative().term().isEmpty()

        if (isSatisfiable) {
            val disj = DisjunctionRxGene("disj", altRes.genes.map { it as Gene }, true, true)
            res.genes.add(disj)
        }
        // else: unsatisfiable, skip that alternative

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
                    // no gene, term and alternative are unsatisfiable
                    ?: return VisitResult()

            res.genes.add(gene)
        }

        return res
    }

    override fun visitTerm(ctx: PostgresSimilarToParser.TermContext): VisitResult {

        val res = VisitResult()

        val resAtom = ctx.atom().accept(this)

        val atom = resAtom.genes.firstOrNull()

        if(ctx.quantifier() != null){

            val limits = ctx.quantifier().accept(this).data as Pair<Int,Int>

            // if quantified atom is unsatisfiable we must then check the limits
            if(atom == null || (atom as? RxTerm)?.isUnsatisfiable() == true){
                return if (limits.first == 0) {
                    // if 0 appearances is allowed then the regex is satisfiable only with empty string
                    VisitResult(PatternCharacterBlockGene("0_QuantifierOnEmptyRegex", ""))
                } else {
                    // if not then unsatisfiable, return with no genes
                    res
                }
            }

            val q = QuantifierRxGene("q", atom, limits.first, limits.second)

            res.genes.add(q)

        } else if (atom != null) {
            res.genes.add(atom)
        }
        // else atom is unsatisfiable, return no genes

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

            val gene = PatternCharacterBlockGene("block", block)

            return VisitResult(gene)
        }


        if(ctx.disjunction() != null){

            val res = ctx.disjunction().accept(this)

            val satisfiableDisjunctions = res.genes
                .map { it as DisjunctionRxGene }
                .filter { !it.isUnsatisfiable() }

            if (satisfiableDisjunctions.isEmpty()) {
                // the group, and so the term, cannot match: no genes
                return VisitResult()
            }

            val disjList = DisjunctionListRxGene(satisfiableDisjunctions)

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

        val ranges = ctx.classRanges().accept(this).data as List<CharacterRange>

        // in Postgres, a ] right after [ or [^ is a literal, and not the end of the class. So [] and [^] are not a
        // closed class, and Postgres rejects them (brackets [] not balanced)
        if (ranges.isEmpty()) {
            throw IllegalArgumentException("Invalid regular expression: brackets [] not balanced")
        }

        val gene = CharacterRangeRxGene(negated, ranges)

        return VisitResult(gene)
    }

    override fun visitClassRanges(ctx: PostgresSimilarToParser.ClassRangesContext): VisitResult {

        val res = VisitResult()
        val list = mutableListOf<CharacterRange>()

        if(ctx.nonemptyClassRanges() != null){
            val ranges = ctx.nonemptyClassRanges().accept(this).data as List<CharacterRange>
            list.addAll(ranges)
        }

        res.data = list

        return res
    }

    override fun visitNonemptyClassRanges(ctx: PostgresSimilarToParser.NonemptyClassRangesContext): VisitResult {

        val list = mutableListOf<CharacterRange>()

        val startText = ctx.classAtom()[0].text
        assert(startText.length == 1) // single chars
        val start : Char = startText[0]

        val end = if(ctx.classAtom().size == 2){
            ctx.classAtom()[1].text[0]
        } else {
            //single char, not an actual range
            start
        }

        list.add(CharacterRange(start, end))

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


    override fun visitNonemptyClassRangesNoDash(ctx: PostgresSimilarToParser.NonemptyClassRangesNoDashContext): VisitResult {

        val list = mutableListOf<CharacterRange>()

        if(ctx.MINUS() != null){

            val start = ctx.classAtomNoDash().text[0]
            val end = ctx.classAtom().text[0]
            list.add(CharacterRange(start, end))

        } else {

            val char = (ctx.classAtom() ?: ctx.classAtomNoDash()).text[0]
            list.add(CharacterRange(char, char))
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
}
