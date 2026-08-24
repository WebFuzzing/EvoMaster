package org.evomaster.dbconstraint.parser.jsql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.SqlComparisonCondition;
import org.evomaster.dbconstraint.ast.SqlComparisonOperator;
import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.parser.SqlConditionParser;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;

import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSqlConditionParser implements SqlConditionParser {

    private static Logger log = LoggerFactory.getLogger(JSqlConditionParser.class);

    private static ExecutorService executor = Executors.newCachedThreadPool();


    /**
     * JSQL does not support legal check constraints such as (x=35) = (y=32).
     * In order to support those constraints, it is possible to split the constraint into
     * two separate formulas (i.e. "x=35" and "y=32") and feed the basic formulas
     * to the JSQL parser. The pattern below allows one to split the "($1)=($2)" string
     * into those two formulas by using the Matcher.group(int) method
     */
    public static final String FORMULA_EQUALS_FORMULA_PATTERN = "\\(\\s*\\(([^<]*)\\)\\s*=\\s*\\(([^<]*)\\)\\s*\\)";

    @Override
    public SqlCondition parse(String sqlConditionStr, ConstraintDatabaseType databaseType, long timeoutMs) throws SqlConditionParserException{

        Future<SqlCondition> future = executor.submit(() -> parse(sqlConditionStr, databaseType));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new SqlConditionParserException(e);
        } catch (ExecutionException e) {
            if(e.getCause() instanceof SqlConditionParserException){
                throw (SqlConditionParserException) e.getCause();
            };
            throw new SqlConditionParserException(e);
        } catch (TimeoutException e){
            log.warn("Failed to analyze SQL constraints within {} ms: {}", timeoutMs, sqlConditionStr);
            throw new SqlConditionParserException(e);
        }
    }

    @Override
    public SqlCondition parse(String sqlConditionStr, ConstraintDatabaseType databaseType) throws SqlConditionParserException {
        try {
            Pattern pattern = Pattern.compile(FORMULA_EQUALS_FORMULA_PATTERN);
            Matcher matcher = pattern.matcher(sqlConditionStr);
            if (matcher.find()) {
                String left = String.format("(%s)", matcher.group(1));
                SqlCondition leftCondition = parse(left, databaseType);
                String right = String.format("(%s)", matcher.group(2));
                SqlCondition rightCondition = parse(right, databaseType);
                return new SqlComparisonCondition(leftCondition, SqlComparisonOperator.EQUALS_TO, rightCondition);
            }
            String transformedSql = transformDialect(sqlConditionStr, databaseType);
            Expression expression = CCJSqlParserUtil.parseCondExpression(transformedSql, false);
            JSqlVisitor translateToSqlCondition = new JSqlVisitor();
            expression.accept(translateToSqlCondition);
            return translateToSqlCondition.getSqlCondition();
        } catch (JSQLParserException e) {
            throw new SqlConditionParserException(e);
        }
    }

    /**
     * Rewrites dialect constructs into constructs the rest of the pipeline can consume.
     *
     * <p>Each rule below exists for one of two reasons, and the distinction decides whether it can
     * ever be removed. Some constructs JSQLParser cannot parse at all. Others parse fine but produce
     * AST nodes {@link JSqlVisitor} does not translate, and no parser upgrade changes that. Each
     * rule states which case it is.
     *
     * @param originalSqlStr original string before transforming dialect primitives
     * @return the transformed SQL so JSQLParser can handle it
     */
    private String transformDialect(String originalSqlStr, ConstraintDatabaseType databaseType) {
        /*
         * How PostgreSQL reports an enumerated column, in one piece:
         *
         *     (col)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying])::text[])
         *
         * Handled ahead of the general rules below because the two of them, applied to this shape,
         * used to leave the cast's own brackets unbalanced: the ARRAY rule ran to the last "]" in the
         * string, which belongs to "::text[]" rather than to the array, and turned it into "::text[".
         * The result was invalid SQL, so the constraint was discarded — but only after JSQLParser had
         * spent a very long time backtracking over it. On one real schema, 44 of 48 check constraints
         * failed this way and cost over five minutes of the search budget between them, with a single
         * 24 KB constraint accounting for 275 seconds of that.
         *
         * A parser limitation: JSQLParser 4.9 rejects the parenthesised expression inside ANY,
         * failing on "ANY ((" before it reaches the array.
         *
         * TODO A parser that accepts the shape does not by itself make this rule removable. The
         * expression would then be an EqualsTo over an ANY function, which JSqlVisitor does not
         * translate either, so both have to handle it first.
         */
        String transformedStr = originalSqlStr.replaceAll(
                "=\\s*ANY\\s*\\(\\s*\\(\\s*ARRAY\\s*\\[([^\\]]*)\\]\\s*\\)\\s*::\\s*\\w+\\s*\\[\\s*\\]\\s*\\)",
                " IN ($1)");

        /*
         * PostgreSQL's "= ANY (...)" is rewritten to the equivalent " IN (...)".
         *
         * Not a parser limitation: JSQLParser 4.9 parses "col = ANY (ARRAY['A','B'])" without
         * complaint, as an EqualsTo whose right side is a Function named ANY. JSqlVisitor is what
         * cannot handle it: visit(Function) only unwraps LOWER and UPPER, and throws for anything
         * else. The rewrite lands the expression on InExpression instead, which it does support.
         *
         * TODO Upgrading JSQLParser has no effect on this rule. It can be dropped once
         * visit(Function) unwraps ANY and visit(EqualsTo) folds a list right-hand side into an
         * SqlInCondition.
         */
        transformedStr = transformedStr.replaceAll("=\\s*ANY\\s*\\(([^<]*)\\)", " IN ($1)");


        /*
         * The Postgres "ARRAY[...]" wrapper is dropped, leaving the bare element list. Used within
         * an enumeration, the elements alone carry the whole meaning.
         *
         * The group excludes "]" so the match ends at the array's own closing bracket rather than
         * at the last one anywhere in the string.
         *
         * Not a parser limitation either: JSQLParser 4.9 parses "x = ARRAY[1,2]" fine, as an
         * EqualsTo whose right side is an ArrayConstructor. JSqlVisitor throws "not yet
         * implemented" from visit(ArrayConstructor).
         *
         * TODO Upgrading JSQLParser has no effect here either. It can be dropped once
         * visit(ArrayConstructor) pushes the element list, which is the same SqlConditionList that
         * visit(ExpressionList) already builds for an IN.
         */
        transformedStr =  transformedStr.replaceAll("ARRAY\\s*\\[([^\\]]*)\\]", "$1");

        /*
         * MySQL Enum.
         *
         * A parser limitation: JSQLParser 4.9 gives up on the shape MySQL reports, stopping at
         * "status enum('a','b')" with "could only parse partial expression".
         *
         * TODO A parser that accepts it still leaves a function call JSqlVisitor does not
         * translate, so the rule can only go once both handle it.
         */
        if (databaseType == ConstraintDatabaseType.MYSQL)
            transformedStr = transformedStr.replaceAll("\\s*[E|e][N|n][U|u][M|m]\\s*\\(([^<]*)\\)", " IN ($1)");

        /*
         * H2 casts expressions to [CHARACTER LARGE OBJECT] instead of [VARCHAR]. We replace
         * CHARACTER LARGE OBJECT with VARCHAR (this could fail if CHARACTER LARGE OBJECT appears
         * inside a string literal).
         *
         * A parser limitation: JSQLParser 4.9 rejects the CHARACTER LARGE OBJECT token outright,
         * on either side of the comparison.
         *
         * TODO Removable once JSQLParser parses the token. The replacement, and with it the
         * literal-corrupting edge case above, would no longer be needed.
         */
        if (databaseType ==ConstraintDatabaseType.H2) {
            transformedStr = transformedStr.replaceAll("CHARACTER LARGE OBJECT","VARCHAR");
        }

        return transformedStr;
    }
}
