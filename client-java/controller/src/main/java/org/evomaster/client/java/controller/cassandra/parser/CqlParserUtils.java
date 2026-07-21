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

    private static final String KEYWORD_SELECT = "SELECT";
    private static final String KEYWORD_UPDATE = "UPDATE";
    private static final String KEYWORD_DELETE = "DELETE";

    /**
     * Parses a CQL command string into its ANTLR parse tree, with no validity checks
     * performed on the result (assumes {@link #canParseCqlCommand} was used first).
     *
     * @param cqlCommand the CQL command to parse
     * @return the root of the resulting ANTLR parse tree
     */
    public static CqlParser.RootContext parseCqlCommand(String cqlCommand) {
        CqlLexer lexer   = new CqlLexer(CharStreams.fromString(cqlCommand));
        CqlParser parser = new CqlParser(new CommonTokenStream(lexer));
        return parser.root();
    }

    /**
     * Checks whether {@code cqlCommand} can be parsed into a single, well-formed CQL statement.
     *
     * @param cqlCommand the CQL command to validate
     * @return {@code true} if the command parses cleanly (no parser exception, and exactly one
     *         statement is present); {@code false} otherwise
     */
    public static boolean canParseCqlCommand(String cqlCommand) {
        try {
            CqlParser.RootContext root = parseCqlCommand(cqlCommand);
            return root.exception == null && root.cqls() != null && root.cqls().cql(0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param cqlCommand the CQL command to inspect
     * @return {@code true} if {@code cqlCommand} is a SELECT statement
     */
    public static boolean isSelect(String cqlCommand) {
        return cqlCommand.trim().toUpperCase().startsWith(KEYWORD_SELECT);
    }

    /**
     * @param cqlCommand the CQL command to inspect
     * @return {@code true} if {@code cqlCommand} is an UPDATE statement
     */
    public static boolean isUpdate(String cqlCommand) {
        return cqlCommand.trim().toUpperCase().startsWith(KEYWORD_UPDATE);
    }

    /**
     * @param cqlCommand the CQL command to inspect
     * @return {@code true} if {@code cqlCommand} is a DELETE statement
     */
    public static boolean isDelete(String cqlCommand) {
        return cqlCommand.trim().toUpperCase().startsWith(KEYWORD_DELETE);
    }

    /**
     * Extracts the WHERE-clause parse-tree node from a parsed CQL SELECT, UPDATE, or DELETE
     * statement.
     *
     * @param root the root of a parsed CQL command, as returned by {@link #parseCqlCommand}
     * @return the WHERE clause's parse-tree node, or {@code null} if the statement has none
     *         (or isn't a SELECT/UPDATE/DELETE)
     */
    public static CqlParser.WhereSpecContext getWhereSpec(CqlParser.RootContext root) {
        CqlParser.CqlContext cql = root.cqls() != null ? root.cqls().cql(0) : null;
        if (cql == null) {
            return null;
        } else if (cql.select_() != null) {
            return cql.select_().whereSpec();
        } else if (cql.update()  != null) {
            return cql.update().whereSpec();
        } else if (cql.delete_() != null) {
            return cql.delete_().whereSpec();
        } else {
            return null;
        }
    }

    /**
     * Parses the WHERE clause of a CQL SELECT, UPDATE, or DELETE and returns the
     * corresponding operation tree. Multiple AND-joined conditions are wrapped in
     * an {@link AndOperation}; a single condition is returned directly; null is
     * returned when no WHERE clause is present.
     */
    public static CqlQueryOperation getWhereOperation(CqlParser.RootContext root) {
        CqlParser.WhereSpecContext whereSpec = getWhereSpec(root);
        if (whereSpec != null) {
            List<CqlParser.RelationElementContext> elements = whereSpec.relationElements().relationElement();
            if (elements.isEmpty()) {
                return null;
            } else if (elements.size() == 1) {
                return parseRelationElement(elements.get(0));
            } else {
                List<CqlQueryOperation> ops = new ArrayList<>();
                for (CqlParser.RelationElementContext el : elements) {
                    CqlQueryOperation op = parseRelationElement(el);
                    if (op != null) {
                        ops.add(op);
                    }
                }
                return new AndOperation(ops);
            }
        } else {
            return null;
        }
    }

    private static CqlQueryOperation parseRelationElement(CqlParser.RelationElementContext rel) {
        if (rel.relalationContainsKey() != null) { // CONTAINS KEY
            CqlParser.RelalationContainsKeyContext ck = rel.relalationContainsKey();
            return new ContainsKeyOperation<>(ck.OBJECT_NAME().getText(), parseConstant(ck.constant()));
        } else if (rel.relalationContains() != null) { // CONTAINS
            CqlParser.RelalationContainsContext c = rel.relalationContains();
            return new ContainsOperation<>(c.OBJECT_NAME().getText(), parseConstant(c.constant()));
        } else if (rel.kwIn() != null) { // IN
            String col = rel.OBJECT_NAME(0).getText();
            List<Object> values = new ArrayList<>();

            if (rel.functionArgs() != null) {
                for (CqlParser.ConstantContext cc : rel.functionArgs().constant()) {
                    values.add(parseConstant(cc));
                }
            }
            return new InOperation(col, values);
        } else {
            // Comparison: col OP constant
            CqlParser.ConstantContext constant = rel.constant();
            if (constant != null && rel.OBJECT_NAME(0) != null) {
                String col   = rel.OBJECT_NAME(0).getText();
                Object value = parseConstant(constant);
                if (rel.OPERATOR_EQ()  != null) {
                    return new EqualsOperation<>(col, value);
                } else if (rel.OPERATOR_GT()  != null) {
                    return new GreaterThanOperation<>(col, value);
                } else if (rel.OPERATOR_GTE() != null) {
                    return new GreaterThanEqualsOperation<>(col, value);
                } else if (rel.OPERATOR_LT()  != null) {
                    return new LessThanOperation<>(col, value);
                } else if (rel.OPERATOR_LTE() != null) {
                    return new LessThanEqualsOperation<>(col, value);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private static Object parseConstant(CqlParser.ConstantContext ctx) {
        if (ctx.UUID() != null) {
            return UUID.fromString(ctx.UUID().getText());
        } else if (ctx.stringLiteral() != null) {
            String raw = ctx.stringLiteral().getText();
            return raw.substring(1, raw.length() - 1); // strip surrounding single quotes
        } else if (ctx.decimalLiteral() != null) {
            return Long.parseLong(ctx.decimalLiteral().getText());
        } else if (ctx.floatLiteral() != null) {
            return Double.parseDouble(ctx.floatLiteral().getText());
        } else if (ctx.booleanLiteral() != null) {
            return ctx.booleanLiteral().getText().equalsIgnoreCase("true");
        } else if (ctx.durationLiteral() != null) {
            return ctx.durationLiteral().getText();
        } else {
            return null; // kwNull, codeBlock, hexadecimal
        }
    }
}