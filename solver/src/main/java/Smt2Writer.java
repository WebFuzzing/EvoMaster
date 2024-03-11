import kotlin.Pair;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.TableCheckExpressionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.*;
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;

/**
 * A writer for SMT2 format.
 * It is used to write the constraints in a file that can be read by any smt2 solver.
 * The parsing and the writing of the constraints is done hardcoded here and not using a library.
 * This is because for the purpose of the feature, it is not necessary to use an external library.
 */
public class Smt2Writer  {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Smt2Writer.class.getName());

    // The variables that solve the constraint, the key is the variable name and the value is the type in smt format (e.g. "x" -> "Int")
    private final Map<String, String> variables = new HashMap<>();

    // The assertions that those variables need to satisfy in smt format (e.g. "(> x 5)")
    private final List<String> constraints = new ArrayList<>();
    private final JSqlConditionParser parser;
    private final ConstraintDatabaseType dbType;

    Smt2Writer(DatabaseType databaseType) {
        this.parser = new JSqlConditionParser();
        this.dbType = ConstraintDatabaseType.valueOf(databaseType.name());
    }

    /**
     * Writes the content of the Smt2Writer in a file with the given filename
     * @param filename the name of the file
     */
    public void writeToFile(String filename) {
        String text = asText();
        try {
            Files.write(Paths.get(filename), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String asText() {
        StringBuilder sb = new StringBuilder();

        sb.append("(set-logic QF_SLIA)\n");

        declareConstants(sb);
        assertConstraints(sb);

        sb.append("(check-sat)\n");
        getValues(sb);

        return sb.toString();
    }

    private void declareConstants(StringBuilder sb) {
        for (String value: this.variables.keySet()) {
            sb.append("(declare-fun ");
            sb.append(value); // variable name
            sb.append(" () ");
            sb.append(this.variables.get(value)); // type
            sb.append(")\n");
        }
    }

    private void assertConstraints(StringBuilder sb) {
        for (String constraint : this.constraints) {
            sb.append("(assert ");
            sb.append(constraint);
            sb.append(")\n");
        }
    }

    private void getValues(StringBuilder sb) {
        for (String value : this.variables.keySet()) {
            sb.append("(get-value (");
            sb.append(value);
            sb.append("))\n");
        }
    }

    public boolean addTableCheckExpression(TableDto table, TableCheckExpressionDto checkConstraint) {
        try {
            SqlCondition condition = parser.parse(checkConstraint.sqlCheckExpression, this.dbType);
            Pair<Map<String, String>, String> response = parseCheckExpression(table, condition);
            this.variables.putAll(response.getFirst());
            this.constraints.add(response.getSecond());
            return true;
        } catch (Exception e) {
            log.error(String.format("There was an error parsing the constraint %s", e.getMessage()));
            return false;
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
    private Pair<Map<String, String>, String> parseCheckExpression(TableDto tableDto, SqlCondition condition) {

            if (condition instanceof SqlAndCondition) {
                SqlAndCondition andCondition = (SqlAndCondition) condition;
                Pair<Map<String, String>, String> leftResponse = parseCheckExpression(tableDto, andCondition.getLeftExpr());
                Pair<Map<String, String>, String> rightResponse = parseCheckExpression(tableDto, andCondition.getRightExpr());

                Map<String, String> variablesAndType = new HashMap<>();
                variablesAndType.putAll(leftResponse.getFirst());
                variablesAndType.putAll(rightResponse.getFirst());

                String comparison = "(and " + leftResponse.getSecond() + " " + rightResponse.getSecond() + ")";

                return new Pair<>(variablesAndType, comparison);
            }

            if (condition instanceof SqlOrCondition) {
                SqlOrCondition orCondition = (SqlOrCondition) condition;
                List<SqlCondition> conditions = orCondition.getOrConditions();
                List<String> orMembers = new ArrayList<>();
                for (SqlCondition c : conditions) {
                    Pair<Map<String, String>, String> response = parseCheckExpression(tableDto, c);
                    variables.putAll(response.getFirst());
                    orMembers.add(response.getSecond());
                }

                return new Pair<>(variables, toOr(orMembers));
            }

            if (condition instanceof SqlInCondition) {
                SqlInCondition inCondition = (SqlInCondition) condition;

                String columnName = inCondition.getSqlColumn().getColumnName();
                String variable = asTableVariableKey(tableDto, columnName);
                Map<String, String> variables = new HashMap<>();
                String variableType = getSmtTypeFromDto(tableDto, columnName);
                variables.put(variable, toSmtType(variableType));

                List<SqlCondition> expressions = inCondition.getLiteralList().getSqlConditionExpressions();
                String assertion = inArrayExpressionToOrAssert(variable, expressions);

                return new Pair<>(variables, assertion);
            }

            if (!(condition instanceof SqlComparisonCondition)) {
                // TODO: Support other check expressions
                throw new RuntimeException("The condition is not a comparison condition");
            }
            SqlComparisonCondition comparisonCondition = (SqlComparisonCondition) condition;

            String columnName = comparisonCondition.getLeftOperand().toString();
            String variable = asTableVariableKey(tableDto, columnName);
            String compare = comparisonCondition.getRightOperand().toString();
            String comparator = comparisonCondition.getSqlComparisonOperator().toString();

            Map<String, String> variables = new HashMap<>();
            String variableType = getSmtTypeFromDto(tableDto, columnName);
            variables.put(variable, toSmtType(variableType));

            return new Pair<>(variables, "(" + comparator + " " + variable + " " + compare + ")");
    }

    private static String getSmtTypeFromDto(TableDto table, String columnName) {
        Optional<String> variableType = table.columns.stream().filter(c -> c.name.equals(columnName)).findFirst().map(c -> c.type);
        if (!variableType.isPresent()) {
            throw new RuntimeException("The condition contains a column not defined in table");
        }
        return variableType.get();
    }

    private String toSmtType(String sqlType) {
        switch (sqlType)   {
            case "INTEGER":
            case "BIGINT":
            case "LONG":
                return "Int";
            case "FLOAT":
            case "DOUBLE":
                return "Real";
        }

        throw new RuntimeException(MessageFormat.format("The sql type ''{0}'' is not supported", sqlType));
    }

    private String inArrayExpressionToOrAssert(String variable, List<SqlCondition> expressions) {
        if (expressions.isEmpty()) return "";
        if (expressions.size() == 1) {
            return "(= " + variable + " " + expressions.get(0).toString() + ")";
        }
        return "(or " +
                inArrayExpressionToOrAssert(variable, Collections.singletonList(expressions.get(expressions.size() - 1))) +
                " " +
                inArrayExpressionToOrAssert(variable, expressions.subList(0, expressions.size() - 1)) +
                ")";
    }

    private String toOr(List<String> orMembers) {
        if (orMembers.isEmpty())
            throw new RuntimeException("The or condition is empty");

        if (orMembers.size() == 1)
            return orMembers.get(0);

        if (orMembers.size() == 2)
            return "(or " + orMembers.get(0) + " " + orMembers.get(1) + ")";

        return "(or " + orMembers.get(orMembers.size() - 1) + " " + toOr(orMembers.subList(0, orMembers.size() - 1)) + ")";
    }

     String asTableVariableKey(TableDto table, String variable) {
        return table.name + "_" + variable;
    }
}
