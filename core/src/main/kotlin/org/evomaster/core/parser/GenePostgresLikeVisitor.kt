package org.evomaster.core.parser

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.*
import java.lang.IllegalArgumentException

/**
 * Visitor for the PostgresLike.g4 grammar
 *
 * Created by arcuri82 on 12-Jun-19.
 */
class GenePostgresLikeVisitor : PostgresLikeBaseVisitor<VisitResult>() {

    override fun visitPattern(ctx: PostgresLikeParser.PatternContext): VisitResult {

        val terms = mutableListOf<Gene>()

        for(i in 0 until ctx.term().size) {

            val resTerm = ctx.term()[i].accept(this)
            val gene = resTerm.genes.firstOrNull()

            if (gene != null) {
                terms.add(gene)
            }
        }

        val disjunction = DisjunctionRxGene("disj", terms, true, true)

        val text = RegexUtils.getRegexExpByParserRuleContext(ctx)

        val gene = RegexGene(
            "LIKE",
            DisjunctionListRxGene(listOf(disjunction)),
            text,
            RegexType.POSTGRES_LIKE
        )

        return VisitResult(gene)
    }

    override fun visitTerm(ctx: PostgresLikeParser.TermContext): VisitResult {

        if(ctx.specialSymbol() != null){

            val symbol = ctx.specialSymbol().text
            return when(symbol){
                "_" -> VisitResult(AnyCharacterRxGene())
                "%" -> VisitResult(QuantifierRxGene("q", AnyCharacterRxGene(), 0, Int.MAX_VALUE))
                else -> throw IllegalArgumentException("Unsupported symbol: $symbol")
            }

        } else {

            // an escaped symbol represents the symbol itself, so "\%" should match "%"
            val block = ctx.baseSymbol().joinToString("") {
                when {
                    it.ESCAPED_PERCENT() != null -> "%"
                    it.ESCAPED_UNDERSCORE() != null -> "_"
                    else -> it.text
                }
            }
            val name = block.ifBlank { "blankBlock" }
            return VisitResult(PatternCharacterBlockGene(name, block))
        }

    }
}
