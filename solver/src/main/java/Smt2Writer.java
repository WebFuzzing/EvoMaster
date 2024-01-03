import org.evomaster.client.java.sql.internal.constraint.DbTableCheckExpression;
import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A writer for SMT2 format.
 * It is used to write the constraints in a file that can be read by any smt2 solver.
 * The parsing and the writing of the constraints is done hardcoded here and not using a library.
 * This is because for the purpose of the feature, it is not necessary to use an external library.
 */
public class Smt2Writer  {
    public static final String CHECK_INT_COMPARE_REGEX = "^CHECK\\(([a-zA-Z_][a-zA-Z0-9_]+)([<|>|=]=?)(.+)\\)$";

    // The variables that solve the constraint
    List<String> variables = new ArrayList<>();

    // The assertions that those values need to satisfy
    List<String> constraints = new ArrayList<>();

    /**
     * Tries to parse the constraint from the DBConstraint, if succeeds returns true
     */
    public boolean addConstraint(DbTableConstraint constraint) {
        try {
            if (constraint instanceof DbTableCheckExpression) {
                String expression = ((DbTableCheckExpression) constraint)
                        .getSqlCheckExpression().trim()
                        .replaceAll(" ", "");

                // TODO: Add support for all other constraints here
                final Matcher matcher = getCheckMatcher(expression);

                variables.add(getVariableFromExpression(matcher));
                constraints.add(getConstraintFromExpressionAsText(matcher));

                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the variable name of the parsed expression in matcher
     * @param matcher the matcher that contains the parsed expression
     * @return the variable name
     */
    private String getVariableFromExpression(Matcher matcher) {
        return matcher.group(1);
    }

    /**
     * Transforms a CHECK constraint into a constraint in smt2 format.
     * For example: "(price>0)" to "(> price 0)".
     * TODO: Add support for more than one value in the expression
     * @param matcher the matcher that contains the parsed expression
     * @return the constraint in smt2 format
     */
    private String getConstraintFromExpressionAsText(Matcher matcher) {
        String variable = matcher.group(1);
        String comparator = matcher.group(2);
        String compare = matcher.group(3);

        return "(" + comparator + " " + variable + " " + compare + ")";
    }

    /**
     * Extracts from expression the comparison parts, so it can be converted into smt2 format
     * Example: Convert CHECK "(price>0)" to "(> price 0)"
     */
    private static Matcher getCheckMatcher(String expression) {
        final Pattern pattern = Pattern.compile(CHECK_INT_COMPARE_REGEX, Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(expression);

        boolean matches = matcher.find();

        if (!matches) {
            throw new RuntimeException("Check expression does not match the expected format");
        }
        return matcher;
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

    @VisibleForTesting
    String asText() {
        StringBuilder sb = new StringBuilder();

        sb.append("(set-logic QF_LIA)\n");

        declareConstants(sb);
        assertConstraints(sb);

        sb.append("(check-sat)\n");
        getValues(sb);

        return sb.toString();
    }

    private void declareConstants(StringBuilder sb) {
        for (String value: variables) {
            sb.append("(declare-const ");
            sb.append(value);
            sb.append(" Int)\n");
        }
    }

    private void assertConstraints(StringBuilder sb) {
        for (String constraint : constraints) {
            sb.append("(assert ");
            sb.append(constraint);
            sb.append(")\n");
        }
    }

    private void getValues(StringBuilder sb) {
        for (String value : variables) {
            sb.append("(get-value (");
            sb.append(value);
            sb.append("))\n");
        }
    }

}
