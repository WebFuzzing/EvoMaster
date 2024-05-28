import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableCheckExpressionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.expression.Expression;
import org.evomaster.client.java.sql.internal.ParserUtils;
import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.*;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;

public class SMTGenerator {

    private final DbSchemaDto schema;
    private static final Map<String, String> TYPE_MAP = new HashMap<String, String>() {{
        put("BIGINT", "Int");
        put("INTEGER", "Int");
        put("FLOAT", "Real");
        put("DOUBLE", "Real");
        put("CHARACTER VARYING", "String");
        put("CHAR", "String");
    }};
    private final JSqlConditionParser parser;
    private final ConstraintDatabaseType dbType;
    private final Integer numberOfRows = 2;

    public SMTGenerator(DbSchemaDto schemaDto) {
        this.schema = schemaDto;
        this.parser = new JSqlConditionParser();
        this.dbType = ConstraintDatabaseType.valueOf(schemaDto.databaseType.name());
    }

    /**
     * Generates an SMT file based on a SQL query
     * @param sqlQuery SQL query to generate SMT file for
     * @param filePath Path to write the SMT file to
     * @throws IOException If an error occurs while writing the file
     */
    public void generateSMTFile(String sqlQuery, String filePath) throws IOException, JSQLParserException, SqlConditionParserException {
        StringBuilder smt = new StringBuilder();

        // Parse the SQL query to extract the table name and condition
        String tableName = extractTableName(sqlQuery);
        String condition = extractCondition(sqlQuery);

        TableDto table = schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(tableName))
                .findFirst()
                .orElse(null);

        if (table == null) {
            throw new IllegalArgumentException("Table not found in schema: " + tableName);
        }

        // Generate SMT definitions for the table
        generateTableDefinitions(table, smt);

        // Add constraints from the SQL query
        addQueryConstraints(table, condition, smt);

        // Add final SMT lines
        addFinalLines(smt, table.name.toLowerCase());

        // Write to file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(smt.toString());
        }
    }

    private String extractTableName(String sqlQuery) throws JSQLParserException {
        Statement selectStatement = CCJSqlParserUtil.parse(sqlQuery);
        Select select = (Select) selectStatement;
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        Table table = (Table) plainSelect.getFromItem();
        return table.getName();
    }

    private String extractCondition(String sqlQuery) throws JSQLParserException {
        Statement selectStatement = CCJSqlParserUtil.parse(sqlQuery);
        Expression where = ParserUtils.getWhere(selectStatement);
        return where.toString();
    }

    private void generateTableDefinitions(TableDto table, StringBuilder smt) throws SqlConditionParserException {
        String tableName = table.name.substring(0, 1).toUpperCase() + table.name.substring(1).toLowerCase();
        String columnsConcat = table.columns.stream()
                .map(c -> c.name.toLowerCase())
                .reduce((a, b) -> a + "-" + b)
                .orElse("");
        smt.append("(declare-datatypes () ((").append(tableName).append("Row (").append(columnsConcat).append(" ");
        for (ColumnDto column : table.columns) {
            String smtType = TYPE_MAP.get(column.type.toUpperCase());
            if (smtType == null) {
                throw new IllegalArgumentException("Unsupported column type: " + column.type);
            }
            smt.append("(").append(column.name.toUpperCase()).append(" ").append(smtType).append(") ");
        }
        smt.append("))))\n");

        // Declare constants for rows
        for (int i = 1; i <= numberOfRows; i++) {
            smt.append("(declare-const ").append(table.name.toLowerCase()).append(i)
                    .append(" ").append(tableName).append("Row)\n");
        }

        // Add constraints for each column
        for (TableCheckExpressionDto check : table.tableCheckExpressions) {

                SqlCondition condition = parser.parse(check.sqlCheckExpression, this.dbType);
                for (int i = 1; i <= numberOfRows; i++) {
                    String constraint = parseCheckExpression(table, condition, i);
                    smt.append("(assert ").append(constraint).append(")\n");
                }
        }
    }

    /**
     * Parses a check expression and returns a pair with the variables and the comparison
     * The first part of the pair is a map with the variables names as key and their types (in smt) as value (e.g. "x" -> "Int")
     * The second part of the pair is the expression to assert in smt format (e.g. "(> x 5)")
     * @param tableDto the table where the variable is a column of
     * @param condition the condition to parse and translate
     * @return a pair with the variables and the expression
     */
    private String parseCheckExpression(TableDto tableDto, SqlCondition condition, Integer index) {

        if (condition instanceof SqlAndCondition) {
            SqlAndCondition andCondition = (SqlAndCondition) condition;

            SqlComparisonCondition leftComparisonCondition = (SqlComparisonCondition) andCondition.getLeftExpr();
            String left = getAssertFromComparison(tableDto, index, leftComparisonCondition);

            SqlComparisonCondition rightComparisonCondition = (SqlComparisonCondition) andCondition.getRightExpr();
            String right = getAssertFromComparison(tableDto, index, rightComparisonCondition);

            return "(and " + left + " " + right + ")";
        }

        if (condition instanceof  SqlComparisonCondition) {
            SqlComparisonCondition comparisonCondition = (SqlComparisonCondition) condition;
            return getAssertFromComparison(tableDto, index, comparisonCondition);
        }

        // TODO: Support other check expressions
        throw new RuntimeException("The condition is not supported");

    }

    private static String getAssertFromComparison(TableDto tableDto, Integer index, SqlComparisonCondition comparisonCondition) {
        String columnName = comparisonCondition.getLeftOperand().toString();
        String variable = "(" + columnName + " " + tableDto.name.toLowerCase() + index + ")";
        String compare = comparisonCondition.getRightOperand().toString();
        String comparator = comparisonCondition.getSqlComparisonOperator().toString();
        return "(" + comparator + " " + variable + " " + compare + ")";
    }

    private void addQueryConstraints(TableDto table, String condition, StringBuilder smt) {
        String column = condition.split(" ")[0];
        String operator = condition.split(" ")[1];
        String value = condition.split(" ")[2];

        for (int i = 1; i <= numberOfRows; i++) {
            smt.append("(assert (").append(operator).append(" (").append(column.toUpperCase()).append(" ")
                    .append(table.name.toLowerCase()).append(i).append(") ").append(value).append("))\n");
        }
    }

    private void addFinalLines(StringBuilder smt, String tableNameLower) {
        smt.append("(check-sat)\n");
        for (int i = 1; i <= numberOfRows; i++) {
            smt.append("(get-value (").append(tableNameLower).append(i).append("))\n");
        }
    }
}
