package org.evomaster.client.java.controller.dynamodb;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.evomaster.client.java.controller.dynamodb.operations.*;

import java.util.*;

/**
 * Parser for DynamoDB key/filter/condition expression strings.
 * Supported operators/functions align with DynamoDB expression docs:
 * =, &lt;&gt;, &lt;, &lt;=, &gt;, &gt;=, BETWEEN, IN, AND, OR, NOT,
 * attribute_exists, attribute_not_exists, attribute_type,
 * begins_with, contains, size.
 */
public class DynamoDbExpressionParser {

    private Map<String, String> expressionAttributeNames = Collections.emptyMap();
    private Map<String, Object> expressionAttributeValues = Collections.emptyMap();

    /**
     * Parses a DynamoDB expression and converts it into a query-operation tree.
     *
     * @param expression the DynamoDB expression string to parse
     * @param expressionAttributeNames optional map of attribute-name aliases
     * @param expressionAttributeValues optional map of value placeholders
     * @return the parsed operation tree, or {@code null} when the expression is blank
     */
    public QueryOperation parse(
            String expression,
            Map<String, String> expressionAttributeNames,
            Map<String, Object> expressionAttributeValues) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        this.expressionAttributeNames = expressionAttributeNames == null
                ? Collections.emptyMap()
                : expressionAttributeNames;
        this.expressionAttributeValues = expressionAttributeValues == null
                ? Collections.emptyMap()
                : expressionAttributeValues;

        try {
            DynamoDbConditionExpressionLexer lexer = new DynamoDbConditionExpressionLexer(CharStreams.fromString(expression));
            prepareLexer(lexer);

            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            DynamoDbConditionExpressionParser parser = new DynamoDbConditionExpressionParser(tokenStream);
            prepareParser(parser);

            DynamoDbConditionExpressionParser.ExpressionContext tree = parser.expression();
            return new OperationVisitor().visit(tree);
        } catch (ParseCancellationException e) {
            throw new IllegalArgumentException("Invalid DynamoDB expression: " + expression, e);
        }
    }

    /**
     * Configures lexer error handling to fail fast on invalid input.
     *
     * @param lexer the lexer to configure
     */
    private void prepareLexer(Lexer lexer) {
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
    }

    /**
     * Configures parser error handling to fail fast on invalid input.
     *
     * @param parser the parser to configure
     */
    private void prepareParser(Parser parser) {
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
    }

    /**
     * Parses and resolves a field path from Parser context.
     *
     * @param pathContext the parsed path context
     * @return resolved field path with attribute-name aliases expanded
     */
    private String parsePath(DynamoDbConditionExpressionParser.PathContext pathContext) {
        return parseFieldName(pathContext.getText());
    }

    /**
     * Resolves expression-attribute-name aliases in a dotted field path.
     *
     * @param token raw field token from the expression
     * @return resolved field path
     */
    private String parseFieldName(String token) {
        String[] chunks = token.split("\\.");
        List<String> resolved = new ArrayList<>(chunks.length);
        for (String chunk : chunks) {
            int bracket = chunk.indexOf('[');
            String base = bracket >= 0 ? chunk.substring(0, bracket) : chunk;
            String suffix = bracket >= 0 ? chunk.substring(bracket) : "";
            if (base.startsWith("#")) {
                resolved.add(expressionAttributeNames.getOrDefault(base, base) + suffix);
            } else {
                resolved.add(base + suffix);
            }
        }
        return String.join(".", resolved);
    }

    /**
     * Converts a parsed value node into a runtime Java value.
     *
     * @param valueContext parsed value context
     * @return converted Java value
     */
    private Object parseValue(DynamoDbConditionExpressionParser.ValueContext valueContext) {
        if (valueContext == null) {
            return null;
        }
        return new ValueVisitor().visit(valueContext);
    }

    /**
     * Parses a numeric literal into {@link Long} or {@link Double}.
     *
     * @param token numeric literal token
     * @return parsed number, or original token when parsing fails
     */
    private Object parseNumberLiteral(String token) {
        try {
            if (token.contains(".") || token.toLowerCase(Locale.ROOT).contains("e")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        } catch (NumberFormatException ignored) {
            // Keep unknown numeric-like literals as-is.
            return token;
        }
    }

    /**
     * Combines two operations with logical AND, flattening nested AND nodes.
     *
     * @param left left condition
     * @param right right condition
     * @return merged AND operation
     */
    private QueryOperation mergeAnd(QueryOperation left, QueryOperation right) {
        if (left instanceof AndOperation) {
            List<QueryOperation> conditions = new ArrayList<>(((AndOperation) left).getConditions());
            conditions.add(right);
            return new AndOperation(conditions);
        }
        return new AndOperation(Arrays.asList(left, right));
    }

    /**
     * Combines two operations with logical OR, flattening nested OR nodes.
     *
     * @param left left condition
     * @param right right condition
     * @return merged OR operation
     */
    private QueryOperation mergeOr(QueryOperation left, QueryOperation right) {
        if (left instanceof OrOperation) {
            List<QueryOperation> conditions = new ArrayList<>(((OrOperation) left).getConditions());
            conditions.add(right);
            return new OrOperation(conditions);
        }
        return new OrOperation(Arrays.asList(left, right));
    }

    /**
     * Visitor that converts grammar value nodes into Java values.
     */
    private class ValueVisitor extends DynamoDbConditionExpressionBaseVisitor<Object> {

        /** {@inheritDoc} */
        @Override
        public Object visitPlaceholderValue(DynamoDbConditionExpressionParser.PlaceholderValueContext ctx) {
            return expressionAttributeValues.get(ctx.PLACEHOLDER().getText());
        }

        /** {@inheritDoc} */
        @Override
        public Object visitStringValue(DynamoDbConditionExpressionParser.StringValueContext ctx) {
            String token = ctx.STRING_LITERAL().getText();
            return token.substring(1, token.length() - 1);
        }

        /** {@inheritDoc} */
        @Override
        public Object visitNumberValue(DynamoDbConditionExpressionParser.NumberValueContext ctx) {
            return parseNumberLiteral(ctx.NUMBER_LITERAL().getText());
        }

        /** {@inheritDoc} */
        @Override
        public Object visitBooleanValue(DynamoDbConditionExpressionParser.BooleanValueContext ctx) {
            return "TRUE".equalsIgnoreCase(ctx.BOOLEAN_LITERAL().getText());
        }

        /** {@inheritDoc} */
        @Override
        public Object visitNullValue(DynamoDbConditionExpressionParser.NullValueContext ctx) {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Object visitIdentifierValue(DynamoDbConditionExpressionParser.IdentifierValueContext ctx) {
            return ctx.IDENTIFIER().getText();
        }
    }

    /**
     * Visitor that converts grammar predicate nodes into query operations.
     */
    private class OperationVisitor extends DynamoDbConditionExpressionBaseVisitor<QueryOperation> {

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitExpression(DynamoDbConditionExpressionParser.ExpressionContext ctx) {
            return visit(ctx.orExpr());
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitOrExpr(DynamoDbConditionExpressionParser.OrExprContext ctx) {
            QueryOperation left = visit(ctx.andExpr(0));
            for (int i = 1; i < ctx.andExpr().size(); i++) {
                QueryOperation right = visit(ctx.andExpr(i));
                left = mergeOr(left, right);
            }
            return left;
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitAndExpr(DynamoDbConditionExpressionParser.AndExprContext ctx) {
            QueryOperation left = visit(ctx.notExpr(0));
            for (int i = 1; i < ctx.notExpr().size(); i++) {
                QueryOperation right = visit(ctx.notExpr(i));
                left = mergeAnd(left, right);
            }
            return left;
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitNegatedExpr(DynamoDbConditionExpressionParser.NegatedExprContext ctx) {
            return new NotOperation(visit(ctx.notExpr()));
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitPrimaryExpr(DynamoDbConditionExpressionParser.PrimaryExprContext ctx) {
            return visit(ctx.primary());
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitParenthesizedPrimary(DynamoDbConditionExpressionParser.ParenthesizedPrimaryContext ctx) {
            return visit(ctx.orExpr());
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitPredicatePrimary(DynamoDbConditionExpressionParser.PredicatePrimaryContext ctx) {
            return visit(ctx.predicate());
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitAttributeExistsPredicate(DynamoDbConditionExpressionParser.AttributeExistsPredicateContext ctx) {
            return new ExistsOperation(parsePath(ctx.path()), true);
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitAttributeNotExistsPredicate(DynamoDbConditionExpressionParser.AttributeNotExistsPredicateContext ctx) {
            return new ExistsOperation(parsePath(ctx.path()), false);
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitAttributeTypePredicate(DynamoDbConditionExpressionParser.AttributeTypePredicateContext ctx) {
            Object expectedType = parseValue(ctx.value());
            return new TypeOperation(parsePath(ctx.path()), expectedType == null ? null : String.valueOf(expectedType));
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitBeginsWithPredicate(DynamoDbConditionExpressionParser.BeginsWithPredicateContext ctx) {
            return new BeginsWithOperation(parsePath(ctx.path()), parseValue(ctx.value()));
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitContainsPredicate(DynamoDbConditionExpressionParser.ContainsPredicateContext ctx) {
            return new ContainsOperation(parsePath(ctx.path()), parseValue(ctx.value()));
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitSizePredicate(DynamoDbConditionExpressionParser.SizePredicateContext ctx) {
            String field = parsePath(ctx.path());
            DynamoDbComparisonType comparator = DynamoDbComparisonType.fromToken(ctx.comparator().getText());
            Object expectedValue = parseValue(ctx.value());
            return new SizeOperation(field, comparator, expectedValue);
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitBetweenPredicate(DynamoDbConditionExpressionParser.BetweenPredicateContext ctx) {
            String field = parsePath(ctx.path());
            Object lower = parseValue(ctx.value(0));
            Object upper = parseValue(ctx.value(1));
            return new BetweenOperation(field, lower, upper);
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitInPredicate(DynamoDbConditionExpressionParser.InPredicateContext ctx) {
            List<Object> values = new ArrayList<>();
            for (DynamoDbConditionExpressionParser.ValueContext valueContext : ctx.value()) {
                values.add(parseValue(valueContext));
            }
            return new InOperation<>(parsePath(ctx.path()), values);
        }

        /** {@inheritDoc} */
        @Override
        public QueryOperation visitComparisonPredicate(DynamoDbConditionExpressionParser.ComparisonPredicateContext ctx) {
            String field = parsePath(ctx.path());
            DynamoDbComparisonType comparator = DynamoDbComparisonType.fromToken(ctx.comparator().getText());
            Object value = parseValue(ctx.value());
            return comparator.toOperation(field, value);
        }
    }

    /**
     * see <a href="https://stackoverflow.com/questions/18132078/handling-errors-in-antlr4">...</a>
     */
    private static class ThrowingErrorListener extends BaseErrorListener {

        private static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        /** {@inheritDoc} */
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                int charPositionInLine, String msg, RecognitionException e) {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}
