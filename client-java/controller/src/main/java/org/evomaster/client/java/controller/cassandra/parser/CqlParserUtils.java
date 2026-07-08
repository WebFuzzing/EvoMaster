package org.evomaster.client.java.controller.cassandra.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.evomaster.client.java.controller.cassandra.operations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility methods to parse CQL (Cassandra Query Language) commands with the ANTLR-generated
 * {@link CqlParser}, and to extract the information needed for heuristics computation (e.g.
 * the WHERE-clause condition tree) from the resulting parse tree.
 */
public class CqlParserUtils {

    private CqlParserUtils() {}

    public static CqlParser.RootContext parseCqlCommand(String cqlCommand) {
        CqlLexer lexer   = new CqlLexer(CharStreams.fromString(cqlCommand));
        CqlParser parser = new CqlParser(new CommonTokenStream(lexer));
        return parser.root();
    }

    public static boolean canParseCqlCommand(String cqlCommand) {
        try {
            CqlParser.RootContext root = parseCqlCommand(cqlCommand);
            return root.exception == null && root.cqls() != null && root.cqls().cql(0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSelect(String cqlCommand) {
        return cqlCommand.trim().toUpperCase().startsWith("SELECT");
    }

    public static boolean isUpdate(String cqlCommand) {
        return cqlCommand.trim().toUpperCase().startsWith("UPDATE");
    }

    public static boolean isDelete(String cqlCommand) {
        return cqlCommand.trim().toUpperCase().startsWith("DELETE");
    }

    public static CqlParser.WhereSpecContext getWhereSpec(CqlParser.RootContext root) {
        CqlParser.CqlContext cql = root.cqls() != null ? root.cqls().cql(0) : null;
        if (cql == null)          return null;
        if (cql.select_() != null) return cql.select_().whereSpec();
        if (cql.update()  != null) return cql.update().whereSpec();
        if (cql.delete_() != null) return cql.delete_().whereSpec();
        return null;
    }

    /**
     * Parses the WHERE clause of a CQL SELECT, UPDATE, or DELETE and returns the
     * corresponding operation tree. Multiple AND-joined conditions are wrapped in
     * an {@link AndOperation}; a single condition is returned directly; null is
     * returned when no WHERE clause is present.
     */
    public static CqlQueryOperation getWhereOperation(CqlParser.RootContext root) {
        CqlParser.WhereSpecContext whereSpec = getWhereSpec(root);
        if (whereSpec == null) return null;

        List<CqlParser.RelationElementContext> elements = whereSpec.relationElements().relationElement();
        if (elements.isEmpty()) return null;
        if (elements.size() == 1) return parseRelationElement(elements.get(0));

        List<CqlQueryOperation> ops = new ArrayList<>();
        for (CqlParser.RelationElementContext el : elements) {
            CqlQueryOperation op = parseRelationElement(el);
            if (op != null) ops.add(op);
        }
        return new AndOperation(ops);
    }

    private static CqlQueryOperation parseRelationElement(CqlParser.RelationElementContext rel) {
        // CONTAINS KEY: col CONTAINS KEY value
        if (rel.relalationContainsKey() != null) {
            CqlParser.RelalationContainsKeyContext ck = rel.relalationContainsKey();
            return new ContainsKeyOperation<>(ck.OBJECT_NAME().getText(), parseConstant(ck.constant()));
        }

        // CONTAINS: col CONTAINS value
        if (rel.relalationContains() != null) {
            CqlParser.RelalationContainsContext c = rel.relalationContains();
            return new ContainsOperation<>(c.OBJECT_NAME().getText(), parseConstant(c.constant()));
        }

        // IN: col IN (v1, v2, ...)
        if (rel.kwIn() != null) {
            String col = rel.OBJECT_NAME(0).getText();
            List<Object> values = new ArrayList<>();
            if (rel.functionArgs() != null) {
                for (CqlParser.ConstantContext cc : rel.functionArgs().constant()) {
                    values.add(parseConstant(cc));
                }
            }
            return new InOperation(col, values);
        }

        // Comparison: col OP constant
        CqlParser.ConstantContext constant = rel.constant();
        if (constant != null && rel.OBJECT_NAME(0) != null) {
            String col   = rel.OBJECT_NAME(0).getText();
            Object value = parseConstant(constant);
            if (rel.OPERATOR_EQ()  != null) return new EqualsOperation<>(col, value);
            if (rel.OPERATOR_GT()  != null) return new GreaterThanOperation<>(col, value);
            if (rel.OPERATOR_GTE() != null) return new GreaterThanEqualsOperation<>(col, value);
            if (rel.OPERATOR_LT()  != null) return new LessThanOperation<>(col, value);
            if (rel.OPERATOR_LTE() != null) return new LessThanEqualsOperation<>(col, value);
        }

        return null;
    }

    private static Object parseConstant(CqlParser.ConstantContext ctx) {
        if (ctx.UUID()            != null) return UUID.fromString(ctx.UUID().getText());
        if (ctx.stringLiteral()   != null) {
            String raw = ctx.stringLiteral().getText();
            return raw.substring(1, raw.length() - 1); // strip surrounding single quotes
        }
        if (ctx.decimalLiteral()  != null) return Long.parseLong(ctx.decimalLiteral().getText());
        if (ctx.floatLiteral()    != null) return Double.parseDouble(ctx.floatLiteral().getText());
        if (ctx.booleanLiteral()  != null) return ctx.booleanLiteral().getText().equalsIgnoreCase("true");
        if (ctx.durationLiteral() != null) return ctx.durationLiteral().getText();
        return null; // kwNull, codeBlock, hexadecimal
    }
}