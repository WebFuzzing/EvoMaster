package org.evomaster.client.java.controller.neo4j.parser.cypher25;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.evomaster.client.java.controller.neo4j.cypher25.Cypher25Parser;
import org.evomaster.client.java.controller.neo4j.cypher25.Cypher25ParserBaseVisitor;
import org.evomaster.client.java.controller.neo4j.conditions.*;
import org.evomaster.client.java.controller.neo4j.operations.*;

import java.util.*;
import java.util.function.Function;

/**
 * Translates the parse tree produced by the Neo4j {@code Cypher25Parser} grammar
 * into the internal {@link MatchOperation} model.
 * <br>
 * Only the rules relevant to MATCH pattern matching are overridden; every other
 * rule falls through to the default {@code visitChildren}, so the visitor stays
 * compact while the underlying grammar remains the full Cypher language.
 * <br>
 * Scope: node/relationship structure, label and relationship-type expressions, inline
 * node/relationship properties, variable-length paths ({@code *1..3}), quantified path
 * patterns ({@code ((a)-[]->(b)){1,3}}, possibly nested), path assignment, and the WHERE
 * clause. Label/type expressions and the WHERE clause are both parsed into a faithful
 * boolean tree ({@link AndCondition} / {@link OrCondition} / {@link XorCondition} /
 * {@link NotCondition}, with parentheses) mirroring the grammar's precedence: {@code (n:A&B|C)}
 * becomes {@code OrCondition[AndCondition[A,B], C]} and {@code [:A|B]} an OR of types, while
 * comparison leaves carry typed {@link Operand}s so both sides of {@code n.age > m.age} stay
 * property references. Predicates not decomposed
 * structurally (functions, bare booleans) are kept unchanged as {@link RawCondition}.
 */
class Cypher25MatchVisitor extends Cypher25ParserBaseVisitor<Void> {

    private final PatternAcc root = new PatternAcc();
    private final List<CypherCondition> conditions = new ArrayList<>();
    /** Where conditions are collected right now; the root list by default, a QPP's own list inside one. */
    private List<CypherCondition> conditionSink = conditions;

    private final List<String> pathVariables = new ArrayList<>();
    private boolean optional;
    private boolean foundMatch;
    private int anonNodeCounter;
    private int anonRelCounter;

    boolean foundMatch() {
        return foundMatch;
    }

    MatchOperation toOperation() {
        return new MatchOperation(root.toPattern(), conditions, pathVariables, optional);
    }

    @Override
    public Void visitMatchClause(Cypher25Parser.MatchClauseContext ctx) {
        foundMatch = true;
        // matchClause : OPTIONAL? MATCH ... — one OPTIONAL clause makes the whole (merged) operation optional.
        if (ctx.OPTIONAL() != null) {
            optional = true;
        }

        for (Cypher25Parser.PatternContext pattern : ctx.patternList().pattern()) {
            processPattern(pattern, root);
        }

        if (ctx.whereClause() != null) {
            processWhere(ctx.whereClause().expression());
        }

        // The clause is fully handled here; do not recurse into it again.
        return null;
    }

    private void processPattern(Cypher25Parser.PatternContext ctx, PatternAcc acc) {
        // pattern : (variable EQ)? selector? anonymousPattern
        if (ctx.variable() != null) {
            pathVariables.add(name(ctx.variable()));
        }

        Cypher25Parser.AnonymousPatternContext anon = ctx.anonymousPattern();
        if (anon.patternElement() != null) {
            processPatternElement(anon.patternElement(), acc);
        } else if (anon.shortestPathPattern() != null) {
            processPatternElement(anon.shortestPathPattern().patternElement(), acc);
        }
    }

    private void processPatternElement(Cypher25Parser.PatternElementContext ctx, PatternAcc acc) {
        // Walk children in source order so nodes/relationships/QPPs splice together correctly:
        // (n0) -rel0- (n1) -rel1- (n2) ... , where a parenthesizedPath (QPP) can stand in for a node.
        String previousNode = null;
        RelInfo pendingRel = null;
        // Index into acc.quantifiedPaths of the most recently added QPP still waiting for its exit
        // variable (the node, or synthesized join, that comes after it); -1 when none is pending.
        int pendingQppIndex = -1;

        for (ParseTree child : children(ctx)) {
            if (child instanceof Cypher25Parser.NodePatternContext) {
                String current = processNode((Cypher25Parser.NodePatternContext) child, acc);
                if (pendingRel != null && previousNode != null) {
                    addEdge(pendingRel, previousNode, current, acc);
                    pendingRel = null;
                }
                if (pendingQppIndex >= 0) {
                    bindQppExit(acc, pendingQppIndex, current);
                    pendingQppIndex = -1;
                }
                previousNode = current;
            } else if (child instanceof Cypher25Parser.RelationshipPatternContext) {
                pendingRel = processRelationship((Cypher25Parser.RelationshipPatternContext) child);
            } else if (child instanceof Cypher25Parser.QuantifierContext && pendingRel != null) {
                applyQuantifier(pendingRel, (Cypher25Parser.QuantifierContext) child);
            } else if (child instanceof Cypher25Parser.ParenthesizedPathContext) {
                QuantifiedPathPattern qpp = processParenthesizedPath((Cypher25Parser.ParenthesizedPathContext) child);
                String entry = previousNode;
                if (entry == null && pendingQppIndex >= 0) {
                    // Two QPPs directly adjacent with no node between them: the grammar still requires
                    // the graph to be continuous there, so synthesize the shared join variable both
                    // QPPs bind to (same mechanism as an anonymous node, just not written by the user).
                    entry = "_anon_qpp_join_" + (anonNodeCounter++);
                    bindQppExit(acc, pendingQppIndex, entry);
                }
                acc.quantifiedPaths.add(qpp.withBoundary(entry, null));
                pendingQppIndex = acc.quantifiedPaths.size() - 1;
                // A QPP can never be followed directly by a relationshipPattern (the grammar only
                // allows one after a nodePattern), so previousNode is only ever read again either by
                // the next nodePattern (which overwrites it unconditionally) or the adjacency check
                // above (which relies on it being null here to detect two QPPs touching).
                previousNode = null;
            }
        }
    }

    /** Rebinds the exit variable of an already-added QPP, keeping its entry variable unchanged. */
    private void bindQppExit(PatternAcc acc, int index, String exitVariable) {
        QuantifiedPathPattern qpp = acc.quantifiedPaths.get(index);
        acc.quantifiedPaths.set(index, qpp.withBoundary(qpp.getEntryVariable(), exitVariable));
    }

    private QuantifiedPathPattern processParenthesizedPath(Cypher25Parser.ParenthesizedPathContext ctx) {
        // parenthesizedPath : LPAREN pattern (WHERE expression)? RPAREN quantifier?
        PatternAcc sub = new PatternAcc();
        // Scope the sub-pattern's labels/types/properties and inline WHERE to this QPP, not the outer
        // list — restore the previous sink afterwards so a nested QPP nests its conditions correctly.
        List<CypherCondition> subConditions = new ArrayList<>();
        List<CypherCondition> previousSink = conditionSink;
        conditionSink = subConditions;
        try {
            processPattern(ctx.pattern(), sub);
            if (ctx.expression() != null) {
                processWhere(ctx.expression()); // inline WHERE filtering the sub-path
            }
        } finally {
            conditionSink = previousSink;
        }

        // A parenthesized path without a quantifier is only a grouping; treat as {1,1}.
        Bounds bounds = ctx.quantifier() != null
                ? quantifierBounds(ctx.quantifier())
                : new Bounds(1, 1);

        return new QuantifiedPathPattern(sub.toPattern(), subConditions, bounds.min, bounds.max);
    }

    private String processNode(Cypher25Parser.NodePatternContext ctx, PatternAcc acc) {
        // nodePattern : LPAREN variable? labelExpression? properties? (WHERE expression)? RPAREN
        String variable = ctx.variable() != null
                ? name(ctx.variable())
                : "_anon_node_" + (anonNodeCounter++);

        acc.nodes.putIfAbsent(variable, new PatternNode(variable));

        if (ctx.labelExpression() != null) {
            addLabelConditions(variable, ctx.labelExpression());
        }
        if (ctx.properties() != null) {
            for (Map.Entry<String, Operand> p : properties(ctx.properties()).entrySet()) {
                conditionSink.add(new PropertyCondition(variable, p.getKey(), p.getValue()));
            }
        }
        if (ctx.expression() != null) {
            processWhere(ctx.expression()); // inline predicate: (n WHERE ...)
        }
        return variable;
    }

    private RelInfo processRelationship(Cypher25Parser.RelationshipPatternContext ctx) {
        // relationshipPattern : leftArrow? arrowLine (LBRACKET variable? labelExpression?
        //                       pathLength? properties? (WHERE expression)? RBRACKET)? arrowLine rightArrow?
        RelInfo rel = new RelInfo();
        rel.leftArrow = ctx.leftArrow() != null;
        rel.directed = rel.leftArrow || ctx.rightArrow() != null;
        rel.variable = ctx.variable() != null
                ? name(ctx.variable())
                : "_anon_rel_" + (anonRelCounter++);

        if (ctx.labelExpression() != null) {
            // Relationship types use the same grammar rule as node labels, so the same
            // faithful boolean tree applies: [:A|B] is an OR of types, [:!A] a negation.
            String relVar = rel.variable;
            rel.typeCondition = labelTree(ctx.labelExpression(), relVar, type -> new TypeCondition(relVar, type));
        }
        if (ctx.pathLength() != null) {
            applyPathLength(rel, ctx.pathLength());
        }
        if (ctx.properties() != null) {
            rel.properties.putAll(properties(ctx.properties()));
        }
        if (ctx.expression() != null) {
            processWhere(ctx.expression()); // inline predicate: -[r WHERE ...]->
        }
        return rel;
    }

    private void addEdge(RelInfo rel, String left, String right, PatternAcc acc) {
        // A left arrow (<-[]-) reverses the logical source/target.
        String source = rel.leftArrow ? right : left;
        String target = rel.leftArrow ? left : right;

        acc.edges.add(new PatternEdge(rel.variable, source, target, rel.directed,
                rel.variableLength, rel.minLength, rel.maxLength));

        if (rel.typeCondition != null) {
            conditionSink.add(rel.typeCondition);
        }
        for (Map.Entry<String, Operand> p : rel.properties.entrySet()) {
            conditionSink.add(new PropertyCondition(rel.variable, p.getKey(), p.getValue()));
        }
    }

    private void addLabelConditions(String variable, Cypher25Parser.LabelExpressionContext ctx) {
        CypherCondition tree = labelTree(ctx, variable, label -> new LabelCondition(variable, label));
        if (tree != null) {
            conditionSink.add(tree);
        }
    }

    /**
     * Builds the boolean tree of a label/type expression following the grammar's precedence:
     * level 4 is OR, 3 is AND ({@code &} or {@code :}), 2 is NOT ({@code !}), 1 is an atom (a
     * name, the {@code %} wildcard, or a parenthesised sub-expression). The two surface forms,
     * {@code :A&B} (COLON) and {@code IS A&B}, have identical structure but distinct generated
     * contexts, so each is walked by its own typed traversal; both share the {@link #or},
     * {@link #and} and {@link #negate} combinators and the {@code leaf} factory, which makes the
     * leaf for a bare name ({@link LabelCondition} for nodes, {@link TypeCondition} for
     * relationships). Returns null for dynamic-label forms.
     */
    private CypherCondition labelTree(Cypher25Parser.LabelExpressionContext ctx,
                                      String variable, Function<String, CypherCondition> leaf) {
        if (ctx.labelExpression4() != null) {
            return buildLabelOr(ctx.labelExpression4(), variable, leaf);
        }
        if (ctx.labelExpression4Is() != null) {
            return buildLabelOrIs(ctx.labelExpression4Is(), variable, leaf);
        }
        return null;
    }

    private CypherCondition or(List<CypherCondition> terms) {
        return terms.size() == 1 ? terms.get(0) : new OrCondition(terms);
    }

    private CypherCondition and(List<CypherCondition> terms) {
        return terms.size() == 1 ? terms.get(0) : new AndCondition(terms);
    }

    private CypherCondition negate(int notCount, CypherCondition inner) {
        // An odd number of '!' negates; an even number cancels out.
        return notCount % 2 == 1 ? new NotCondition(inner) : inner;
    }

    private CypherCondition buildLabelOr(Cypher25Parser.LabelExpression4Context ctx,
                                         String variable, Function<String, CypherCondition> leaf) {
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.LabelExpression3Context p : ctx.labelExpression3()) {
            terms.add(buildLabelAnd(p, variable, leaf));
        }
        return or(terms);
    }

    private CypherCondition buildLabelAnd(Cypher25Parser.LabelExpression3Context ctx,
                                          String variable, Function<String, CypherCondition> leaf) {
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.LabelExpression2Context p : ctx.labelExpression2()) {
            terms.add(buildLabelNot(p, variable, leaf));
        }
        return and(terms);
    }

    private CypherCondition buildLabelNot(Cypher25Parser.LabelExpression2Context ctx,
                                          String variable, Function<String, CypherCondition> leaf) {
        return negate(ctx.EXCLAMATION_MARK().size(), buildLabelAtom(ctx.labelExpression1(), variable, leaf));
    }

    private CypherCondition buildLabelAtom(Cypher25Parser.LabelExpression1Context ctx,
                                           String variable, Function<String, CypherCondition> leaf) {
        if (ctx instanceof Cypher25Parser.ParenthesizedLabelExpressionContext) {
            return buildLabelOr(
                    ((Cypher25Parser.ParenthesizedLabelExpressionContext) ctx).labelExpression4(), variable, leaf);
        }
        if (ctx instanceof Cypher25Parser.AnyLabelContext) {
            return new AnyLabelCondition(variable);
        }
        if (ctx instanceof Cypher25Parser.LabelNameContext) {
            return leaf.apply(name(((Cypher25Parser.LabelNameContext) ctx).symbolicNameString()));
        }
        return new RawCondition(ctx.getText());
    }

    private CypherCondition buildLabelOrIs(Cypher25Parser.LabelExpression4IsContext ctx,
                                           String variable, Function<String, CypherCondition> leaf) {
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.LabelExpression3IsContext p : ctx.labelExpression3Is()) {
            terms.add(buildLabelAndIs(p, variable, leaf));
        }
        return or(terms);
    }

    private CypherCondition buildLabelAndIs(Cypher25Parser.LabelExpression3IsContext ctx,
                                            String variable, Function<String, CypherCondition> leaf) {
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.LabelExpression2IsContext p : ctx.labelExpression2Is()) {
            terms.add(buildLabelNotIs(p, variable, leaf));
        }
        return and(terms);
    }

    private CypherCondition buildLabelNotIs(Cypher25Parser.LabelExpression2IsContext ctx,
                                            String variable, Function<String, CypherCondition> leaf) {
        return negate(ctx.EXCLAMATION_MARK().size(), buildLabelAtomIs(ctx.labelExpression1Is(), variable, leaf));
    }

    private CypherCondition buildLabelAtomIs(Cypher25Parser.LabelExpression1IsContext ctx,
                                             String variable, Function<String, CypherCondition> leaf) {
        if (ctx instanceof Cypher25Parser.ParenthesizedLabelExpressionIsContext) {
            return buildLabelOrIs(
                    ((Cypher25Parser.ParenthesizedLabelExpressionIsContext) ctx).labelExpression4Is(), variable, leaf);
        }
        if (ctx instanceof Cypher25Parser.AnyLabelIsContext) {
            return new AnyLabelCondition(variable);
        }
        if (ctx instanceof Cypher25Parser.LabelNameIsContext) {
            return leaf.apply(name(((Cypher25Parser.LabelNameIsContext) ctx).symbolicLabelNameString()));
        }
        return new RawCondition(ctx.getText());
    }

    private void applyPathLength(RelInfo rel, Cypher25Parser.PathLengthContext ctx) {
        rel.variableLength = true;
        if (ctx.single != null) {
            rel.minLength = parseInt(ctx.single.getText());
            rel.maxLength = rel.minLength;
        } else {
            if (ctx.from != null) rel.minLength = parseInt(ctx.from.getText());
            if (ctx.to != null) rel.maxLength = parseInt(ctx.to.getText());
        }
    }

    private void applyQuantifier(RelInfo rel, Cypher25Parser.QuantifierContext ctx) {
        Bounds bounds = quantifierBounds(ctx);
        rel.variableLength = true;
        rel.minLength = bounds.min;
        rel.maxLength = bounds.max;
    }

    private Bounds quantifierBounds(Cypher25Parser.QuantifierContext ctx) {
        if (ctx.PLUS() != null) {
            return new Bounds(1, null);
        }
        if (ctx.TIMES() != null) {
            return new Bounds(0, null);
        }
        if (ctx.COMMA() != null) {
            int min = ctx.from != null ? parseInt(ctx.from.getText()) : 0;
            Integer max = ctx.to != null ? parseInt(ctx.to.getText()) : null;
            return new Bounds(min, max);
        }
        int exact = parseInt(ctx.UNSIGNED_DECIMAL_INTEGER(0).getText());
        return new Bounds(exact, exact);
    }

    /**
     * Builds the full boolean tree of the WHERE expression and adds it as a single
     * condition. The connective structure (OR / XOR / AND / NOT, with parentheses)
     * is preserved faithfully; leaves are comparisons. Predicates that are not decomposed
     * structurally (functions, bare booleans) are kept unchanged as {@link RawCondition}
     * rather than dropped, so no part of the query is lost.
     */
    private void processWhere(Cypher25Parser.ExpressionContext ctx) {
        CypherCondition where = buildExpression(ctx);
        if (where != null) {
            conditionSink.add(where);
        }
    }

    // expression : expression11 (OR expression11)*
    private CypherCondition buildExpression(Cypher25Parser.ExpressionContext ctx) {
        List<Cypher25Parser.Expression11Context> parts = ctx.expression11();
        if (parts.size() == 1) {
            return buildXor(parts.get(0));
        }
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.Expression11Context p : parts) {
            terms.add(buildXor(p));
        }
        return new OrCondition(terms);
    }

    // expression11 : expression10 (XOR expression10)*
    private CypherCondition buildXor(Cypher25Parser.Expression11Context ctx) {
        List<Cypher25Parser.Expression10Context> parts = ctx.expression10();
        if (parts.size() == 1) {
            return buildAnd(parts.get(0));
        }
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.Expression10Context p : parts) {
            terms.add(buildAnd(p));
        }
        return new XorCondition(terms);
    }

    // expression10 : expression9 (AND expression9)*
    private CypherCondition buildAnd(Cypher25Parser.Expression10Context ctx) {
        List<Cypher25Parser.Expression9Context> parts = ctx.expression9();
        if (parts.size() == 1) {
            return buildNot(parts.get(0));
        }
        List<CypherCondition> terms = new ArrayList<>();
        for (Cypher25Parser.Expression9Context p : parts) {
            terms.add(buildNot(p));
        }
        return new AndCondition(terms);
    }

    // expression9 : NOT* expression8
    private CypherCondition buildNot(Cypher25Parser.Expression9Context ctx) {
        CypherCondition inner = buildComparison(ctx.expression8());
        // An odd number of NOTs negates; an even number cancels out.
        return ctx.NOT().size() % 2 == 1 ? new NotCondition(inner) : inner;
    }

    // expression8 : expression7 ((EQ | NEQ | LE | GE | LT | GT) expression7)*
    private CypherCondition buildComparison(Cypher25Parser.Expression8Context ctx) {
        List<Cypher25Parser.Expression7Context> operands = ctx.expression7();
        if (operands.size() == 1) {
            return buildExpression7(operands.get(0));
        }
        // A chain a op1 b op2 c expands to (a op1 b) AND (b op2 c), as Cypher chains comparisons.
        // Operators are read in source order so a mixed chain (a < b <= c) keeps each one.
        List<ComparisonOperator> operators = comparisonOperators(ctx);
        List<CypherCondition> links = new ArrayList<>();
        for (int i = 0; i < operators.size(); i++) {
            links.add(new ComparisonCondition(
                    operand(operands.get(i)), operators.get(i), operand(operands.get(i + 1))));
        }
        return links.size() == 1 ? links.get(0) : new AndCondition(links);
    }

    /** The comparison operators of an expression8 chain, in source order. */
    private List<ComparisonOperator> comparisonOperators(Cypher25Parser.Expression8Context ctx) {
        List<ComparisonOperator> operators = new ArrayList<>();
        for (ParseTree child : children(ctx)) {
            if (child instanceof TerminalNode) {
                ComparisonOperator op = comparisonOperator(((TerminalNode) child).getSymbol().getType());
                if (op != null) {
                    operators.add(op);
                }
            }
        }
        return operators;
    }

    // expression7 : expression6 comparisonExpression6?
    private CypherCondition buildExpression7(Cypher25Parser.Expression7Context ctx) {
        if (ctx.comparisonExpression6() != null) {
            CypherCondition c = stringOrNullComparison(ctx);
            if (c != null) {
                return c;
            }
        }
        // A parenthesized boolean group, e.g. (a OR b): recurse into the inner expression.
        Cypher25Parser.ExpressionContext nested = enclosedExpression(ctx);
        if (nested != null) {
            return buildExpression(nested);
        }
        // A predicate that is not decomposed structurally (function, bare boolean).
        return new RawCondition(ctx.getText());
    }

    private CypherCondition stringOrNullComparison(Cypher25Parser.Expression7Context ctx) {
        Operand left = operand(ctx.expression6());
        Cypher25Parser.ComparisonExpression6Context cmp = ctx.comparisonExpression6();

        if (cmp instanceof Cypher25Parser.StringAndListComparisonContext) {
            Cypher25Parser.StringAndListComparisonContext s =
                    (Cypher25Parser.StringAndListComparisonContext) cmp;
            ComparisonOperator op = null;
            if (s.STARTS() != null) op = ComparisonOperator.STARTS_WITH;
            else if (s.ENDS() != null) op = ComparisonOperator.ENDS_WITH;
            else if (s.CONTAINS() != null) op = ComparisonOperator.CONTAINS;
            else if (s.IN() != null) op = ComparisonOperator.IN;
            if (op != null) {
                return new ComparisonCondition(left, op, operand(s.expression6()));
            }
        } else if (cmp instanceof Cypher25Parser.NullComparisonContext) {
            Cypher25Parser.NullComparisonContext n = (Cypher25Parser.NullComparisonContext) cmp;
            ComparisonOperator op = n.NOT() != null
                    ? ComparisonOperator.IS_NOT_NULL
                    : ComparisonOperator.IS_NULL;
            return new ComparisonCondition(left, op, (Operand) null);
        }
        return null; // TYPE / NORMALIZED comparison: not modelled
    }

    /**
     * Classifies one side of a comparison: a property reference {@code variable.key} as a
     * {@link PropertyOperand}, a list {@code [..]} as a {@link ListOperand} of element operands,
     * an operand that is exactly a literal as a {@link LiteralOperand}, an arithmetic expression
     * ({@code + - * / % ^}, unary minus) as an {@link ArithmeticOperand} (folded to a literal when
     * every leaf is a numeric literal), and anything else (a function call) as a {@link RawOperand}.
     */
    private Operand operand(ParseTree expression) {
        PropertyOperand property = propertyReference(expression);
        if (property != null) {
            return property;
        }
        Cypher25Parser.ListLiteralContext list = soleList(expression);
        if (list != null) {
            List<Operand> elements = new ArrayList<>();
            for (Cypher25Parser.ExpressionContext element : list.expression()) {
                elements.add(operand(element));
            }
            return new ListOperand(elements);
        }
        Cypher25Parser.LiteralContext literal = soleLiteral(expression);
        if (literal != null) {
            return new LiteralOperand(convertLiteral(literal));
        }
        Operand arithmetic = arithmetic(expression);
        if (arithmetic != null) {
            return arithmetic;
        }
        return new RawOperand(expression.getText());
    }

    /**
     * Builds an {@link ArithmeticOperand} tree for an operand that is an arithmetic expression,
     * or {@code null} when the operand is not arithmetic (so the caller keeps it as a
     * {@link RawOperand}). Sub-operands go back through {@link #operand}, so a nested {@code p.age}
     * stays a {@link PropertyOperand}; an all-numeric-literal subtree folds to a {@link LiteralOperand}.
     */
    private Operand arithmetic(ParseTree expression) {
        return buildArithmetic(descendSingleChild(expression));
    }

    private Operand buildArithmetic(ParseTree node) {
        if (node instanceof Cypher25Parser.ParenthesizedExpressionContext) {
            return operand(((Cypher25Parser.ParenthesizedExpressionContext) node).expression());
        }
        if (node instanceof Cypher25Parser.Expression6Context        // + - || (left-assoc)
                || node instanceof Cypher25Parser.Expression5Context) { // * / % (left-assoc)
            return buildLeftAssoc(node);
        }
        if (node instanceof Cypher25Parser.Expression4Context) {     // ^ (right-assoc)
            return buildPower((Cypher25Parser.Expression4Context) node);
        }
        if (node instanceof Cypher25Parser.Expression3Context) {     // unary + / -
            return buildUnary((Cypher25Parser.Expression3Context) node);
        }
        return null;
    }

    /** Folds {@code operand (OP operand)*} left to right; null if any operator is non-arithmetic (e.g. {@code ||}). */
    private Operand buildLeftAssoc(ParseTree node) {
        Operand acc = null;
        ArithmeticOperator pending = null;
        for (ParseTree child : children(node)) {
            if (child instanceof TerminalNode) {
                pending = arithmeticOperator(((TerminalNode) child).getSymbol().getType());
                if (pending == null) {
                    return null;
                }
            } else {
                Operand current = operand(child);
                acc = acc == null ? current : arithmeticNode(pending, acc, current);
            }
        }
        return acc;
    }

    /** {@code expression3 (POW expression3)*} folded right-associatively, as exponentiation associates. */
    private Operand buildPower(Cypher25Parser.Expression4Context node) {
        List<Cypher25Parser.Expression3Context> parts = node.expression3();
        Operand acc = operand(parts.get(parts.size() - 1));
        for (int i = parts.size() - 2; i >= 0; i--) {
            acc = arithmeticNode(ArithmeticOperator.POWER, operand(parts.get(i)), acc);
        }
        return acc;
    }

    /** {@code (PLUS | MINUS) expression2}: unary minus negates, unary plus is identity. */
    private Operand buildUnary(Cypher25Parser.Expression3Context node) {
        Operand inner = operand(node.expression2());
        return node.MINUS() != null ? arithmeticNode(ArithmeticOperator.NEGATE, inner, null) : inner;
    }

    /** A folded literal when the operands evaluate to one, else a structural {@link ArithmeticOperand}. */
    private Operand arithmeticNode(ArithmeticOperator op, Operand left, Operand right) {
        Operand folded = fold(op, left, right);
        return folded != null ? folded : new ArithmeticOperand(op, left, right);
    }

    /** Evaluates an arithmetic node when every operand is a numeric literal; otherwise null. */
    private Operand fold(ArithmeticOperator op, Operand left, Operand right) {
        Object l = numericLiteral(left);
        if (l == null) {
            return null;
        }
        if (op == ArithmeticOperator.NEGATE) {
            return l instanceof Long
                    ? new LiteralOperand(-(Long) l)
                    : new LiteralOperand(-(Double) l);
        }
        Object r = numericLiteral(right);
        if (r == null) {
            return null;
        }
        return l instanceof Long && r instanceof Long
                ? foldLong(op, (Long) l, (Long) r)
                : foldDouble(op, ((Number) l).doubleValue(), ((Number) r).doubleValue());
    }

    /** Integer arithmetic; leaves the node unfolded (null) on overflow or division by zero. */
    private Operand foldLong(ArithmeticOperator op, long a, long b) {
        try {
            switch (op) {
                case PLUS:   return new LiteralOperand(Math.addExact(a, b));
                case MINUS:  return new LiteralOperand(Math.subtractExact(a, b));
                case TIMES:  return new LiteralOperand(Math.multiplyExact(a, b));
                case DIVIDE: return b == 0 ? null : new LiteralOperand(a / b);
                case MODULO: return b == 0 ? null : new LiteralOperand(a % b);
                case POWER:  return new LiteralOperand(Math.pow(a, b));   // Neo4j's ^ yields a float
                default:     return null;
            }
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    /** Floating-point arithmetic; leaves the node unfolded (null) on division by zero. */
    private Operand foldDouble(ArithmeticOperator op, double a, double b) {
        switch (op) {
            case PLUS:   return new LiteralOperand(a + b);
            case MINUS:  return new LiteralOperand(a - b);
            case TIMES:  return new LiteralOperand(a * b);
            case DIVIDE: return b == 0 ? null : new LiteralOperand(a / b);
            case MODULO: return b == 0 ? null : new LiteralOperand(a % b);
            case POWER:  return new LiteralOperand(Math.pow(a, b));
            default:     return null;
        }
    }

    private Object numericLiteral(Operand operand) {
        if (operand instanceof LiteralOperand) {
            Object value = ((LiteralOperand) operand).getValue();
            if (value instanceof Long || value instanceof Double) {
                return value;
            }
        }
        return null;
    }

    private ArithmeticOperator arithmeticOperator(int tokenType) {
        if (tokenType == Cypher25Parser.PLUS) return ArithmeticOperator.PLUS;
        if (tokenType == Cypher25Parser.MINUS) return ArithmeticOperator.MINUS;
        if (tokenType == Cypher25Parser.TIMES) return ArithmeticOperator.TIMES;
        if (tokenType == Cypher25Parser.DIVIDE) return ArithmeticOperator.DIVIDE;
        if (tokenType == Cypher25Parser.PERCENT) return ArithmeticOperator.MODULO;
        if (tokenType == Cypher25Parser.POW) return ArithmeticOperator.POWER;
        return null;                                              // DOUBLEBAR (||) etc.: not numeric arithmetic
    }

    /** The list literal an operand reduces to when it is nothing but {@code [..]}, else null. */
    private Cypher25Parser.ListLiteralContext soleList(ParseTree expression) {
        ParseTree node = descendSingleChild(expression);
        return node instanceof Cypher25Parser.ListLiteralContext
                ? (Cypher25Parser.ListLiteralContext) node
                : null;
    }

    /** A property reference {@code variable.key}, or null if the operand is not exactly that. */
    private PropertyOperand propertyReference(ParseTree expression) {
        ParseTree node = descendSingleChild(expression);
        if (!(node instanceof Cypher25Parser.Expression2Context)) {
            return null;
        }
        Cypher25Parser.Expression2Context e2 = (Cypher25Parser.Expression2Context) node;
        List<Cypher25Parser.PostFixContext> postfixes = e2.postFix();
        if (postfixes.size() != 1 || !(postfixes.get(0) instanceof Cypher25Parser.PropertyPostfixContext)) {
            return null; // not a single `.key` access (e.g. indexing, or a longer chain)
        }
        Cypher25Parser.VariableContext base = e2.expression1().variable();
        if (base == null) {
            return null; // the base is not a bare variable (e.g. a function result)
        }
        Cypher25Parser.PropertyPostfixContext postfix =
                (Cypher25Parser.PropertyPostfixContext) postfixes.get(0);
        String key = name(postfix.property().propertyKeyName().symbolicNameString());
        return new PropertyOperand(name(base), key);
    }

    /** The literal an operand reduces to when it is nothing but a literal, else null. */
    private Cypher25Parser.LiteralContext soleLiteral(ParseTree tree) {
        ParseTree t = tree;
        while (t != null) {
            if (t instanceof Cypher25Parser.LiteralContext) {
                return (Cypher25Parser.LiteralContext) t;
            }
            if (t.getChildCount() != 1) {
                return null; // a branch (operator, arguments): not a bare literal
            }
            t = t.getChild(0);
        }
        return null;
    }

    /** Follows the single-child chain down to the first node that branches (or a leaf). */
    private ParseTree descendSingleChild(ParseTree tree) {
        ParseTree t = tree;
        while (t.getChildCount() == 1) {
            t = t.getChild(0);
        }
        return t;
    }

    /** Descends a single-child chain; returns the inner expression if it is a parenthesized group. */
    private Cypher25Parser.ExpressionContext enclosedExpression(ParseTree tree) {
        if (tree instanceof Cypher25Parser.ParenthesizedExpressionContext) {
            return ((Cypher25Parser.ParenthesizedExpressionContext) tree).expression();
        }
        if (tree.getChildCount() == 1) {
            return enclosedExpression(tree.getChild(0));
        }
        return null;
    }

    private ComparisonOperator comparisonOperator(int tokenType) {
        if (tokenType == Cypher25Parser.EQ) return ComparisonOperator.EQUALS;
        if (tokenType == Cypher25Parser.NEQ || tokenType == Cypher25Parser.INVALID_NEQ) return ComparisonOperator.NOT_EQUALS;
        if (tokenType == Cypher25Parser.LE) return ComparisonOperator.LESS_THAN_OR_EQUALS;
        if (tokenType == Cypher25Parser.GE) return ComparisonOperator.GREATER_THAN_OR_EQUALS;
        if (tokenType == Cypher25Parser.LT) return ComparisonOperator.LESS_THAN;
        if (tokenType == Cypher25Parser.GT) return ComparisonOperator.GREATER_THAN;
        return null;
    }

    private Map<String, Operand> properties(Cypher25Parser.PropertiesContext ctx) {
        // properties : map | parameter
        Map<String, Operand> result = new LinkedHashMap<>();
        Cypher25Parser.MapContext map = ctx.map();
        if (map == null) {
            return result; // parameterised properties ($props) carry no literal values
        }
        List<Cypher25Parser.PropertyKeyNameContext> keys = map.propertyKeyName();
        List<Cypher25Parser.ExpressionContext> values = map.expression();
        for (int i = 0; i < keys.size(); i++) {
            // A property value is classified exactly like a comparison operand, so it folds
            // constants and keeps functions/parameters as a RawOperand instead of guessing.
            result.put(name(keys.get(i).symbolicNameString()), operand(values.get(i)));
        }
        return result;
    }

    private Object convertLiteral(Cypher25Parser.LiteralContext literal) {
        if (literal instanceof Cypher25Parser.StringsLiteralContext) {
            String raw = literal.getText();
            return raw.length() >= 2 ? raw.substring(1, raw.length() - 1) : raw;
        }
        if (literal instanceof Cypher25Parser.NummericLiteralContext) {
            String number = literal.getText();
            try {
                return number.contains(".") || number.toLowerCase().contains("e")
                        ? (Object) Double.parseDouble(number)
                        : (Object) Long.parseLong(number);
            } catch (NumberFormatException e) {
                return number;
            }
        }
        if (literal instanceof Cypher25Parser.BooleanLiteralContext) {
            return literal.getText().equalsIgnoreCase("true");
        }
        if (literal instanceof Cypher25Parser.KeywordLiteralContext) {
            return literal.getText().equalsIgnoreCase("null") ? null : literal.getText();
        }
        return literal.getText();
    }

    private String name(Cypher25Parser.VariableContext ctx) {
        return name(ctx.symbolicNameString());
    }

    private String name(Cypher25Parser.SymbolicNameStringContext ctx) {
        return unquote(ctx.getText());
    }

    private String name(Cypher25Parser.SymbolicLabelNameStringContext ctx) {
        return unquote(ctx.getText());
    }

    /** Strips the backtick quoting from an escaped identifier, leaving plain ones untouched. */
    private String unquote(String text) {
        if (text.length() >= 2 && text.charAt(0) == '`' && text.charAt(text.length() - 1) == '`') {
            return text.substring(1, text.length() - 1).replace("``", "`");
        }
        return text;
    }

    private List<ParseTree> children(ParseTree parent) {
        List<ParseTree> result = new ArrayList<>(parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            result.add(parent.getChild(i));
        }
        return result;
    }

    private Integer parseInt(String text) {
        return Integer.valueOf(text);
    }

    /** Accumulates the nodes, edges and quantified paths of one (sub-)pattern. */
    private static final class PatternAcc {
        final Map<String, PatternNode> nodes = new LinkedHashMap<>();
        final List<PatternEdge> edges = new ArrayList<>();
        final List<QuantifiedPathPattern> quantifiedPaths = new ArrayList<>();

        MatchPattern toPattern() {
            return new MatchPattern(new ArrayList<>(nodes.values()), edges, quantifiedPaths);
        }
    }

    /** Repetition bounds [min, max]; a null max means unbounded. */
    private static final class Bounds {
        final int min;
        final Integer max;

        Bounds(int min, Integer max) {
            this.min = min;
            this.max = max;
        }
    }

    /** Mutable accumulator for a relationship while its endpoints are being resolved. */
    private static final class RelInfo {
        String variable;
        CypherCondition typeCondition;
        final Map<String, Operand> properties = new LinkedHashMap<>();
        boolean leftArrow;
        boolean directed;
        boolean variableLength;
        Integer minLength;
        Integer maxLength;
    }
}
