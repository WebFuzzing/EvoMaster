package org.evomaster.core.parser.visitor

import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.evomaster.core.parser.RegexEcma262Parser
import org.evomaster.core.parser.RegexEcma262Visitor


class Ecma262Visitor : RegexEcma262Visitor<VisitResult>{

    override fun visitPattern(ctx: RegexEcma262Parser.PatternContext): VisitResult {

        ctx.disjunction().accept(this)

        //TODO
        return VisitResult()
    }

    override fun visitDisjunction(ctx: RegexEcma262Parser.DisjunctionContext): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitTerm(ctx: RegexEcma262Parser.TermContext): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(p0: ParseTree): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAlternative(ctx: RegexEcma262Parser.AlternativeContext): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAssertion(ctx: RegexEcma262Parser.AssertionContext): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAtom(ctx: RegexEcma262Parser.AtomContext): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitChildren(p0: RuleNode): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitErrorNode(p0: ErrorNode): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitTerminal(p0: TerminalNode): VisitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}