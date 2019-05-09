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

public class JSqlConditionParser extends SqlConditionParser {

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
        // replace " = ANY (ARRAY[...])" with " IN (...)"
        return originalSqlStr.replaceAll("=\\s*ANY\\s*\\(\\s*ARRAY\\s*\\[([^<]*)\\]\\s*\\)", " IN ($1)");
    }
}
