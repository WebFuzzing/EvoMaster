package org.evomaster.dbconstraint.parser.jsql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.evomaster.dbconstraint.ast.SqlComparisonCondition;
import org.evomaster.dbconstraint.ast.SqlComparisonOperator;
import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.parser.SqlConditionParser;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSqlConditionParser implements SqlConditionParser {

    /**
     * JSQL does not support legal check constraints such as (x=35) = (y=32).
     * In order to support those constraints, it is possible to split the constraint into
     * two separate formulas (i.e. "x=35" and "y=32") and feed the basic formulas
     * to the JSQL parser. The pattern below allows one to split the "($1)=($2)" string
     * into those two formulas by using the Matcher.group(int) method
     */
    public static final String FORMULA_EQUALS_FORMULA_PATTERN = "\\(\\s*\\(([^<]*)\\)\\s*=\\s*\\(([^<]*)\\)\\s*\\)";

    @Override
    public SqlCondition parse(String sqlConditionStr) throws SqlConditionParserException {
        try {
            Pattern pattern = Pattern.compile(FORMULA_EQUALS_FORMULA_PATTERN);
            Matcher matcher = pattern.matcher(sqlConditionStr);
            if (matcher.find()) {
                String left = String.format("(%s)", matcher.group(1));
                SqlCondition leftCondition = parse(left);
                String right = String.format("(%s)", matcher.group(2));
                SqlCondition rightCondition = parse(right);
                return new SqlComparisonCondition(leftCondition, SqlComparisonOperator.EQUALS_TO, rightCondition);
            }
            String transformedSql = transformDialect(sqlConditionStr);
            Expression expression = CCJSqlParserUtil.parseCondExpression(transformedSql, false);
            JSqlVisitor translateToSqlCondition = new JSqlVisitor();
            expression.accept(translateToSqlCondition);
            return translateToSqlCondition.getSqlCondition();
        } catch (JSQLParserException e) {
            throw new SqlConditionParserException(e);
        }
    }

    /**
     * replaces unsupported grammar of JSQLParser with equivalent supported constructs
     *
     * @param originalSqlStr
     * @return
     */
    private String transformDialect(String originalSqlStr) {
        /**
         * The JSQL parser does not properly parse the Postgresql SQL dialect function "ANY"
         * We can work aroung this limitation by replacing the "= ANY (...)" with a valid " IN (...)"
         * string
         */
        String transformedStr = originalSqlStr.replaceAll("=\\s*ANY\\s*\\(([^<]*)\\)", " IN ($1)");

        /**
         * The JSQL parser does not properly handle the Postgres "ARRAY[...]" construct. Since
         * the ARRAY is used within a enumeration, we can simply drop the "ARRAY[...]"
         */
        return transformedStr.replaceAll("ARRAY\\s*\\[([^<]*)\\]", "$1");
    }
}
