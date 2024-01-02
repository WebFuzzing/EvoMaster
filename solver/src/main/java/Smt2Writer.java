import org.evomaster.client.java.sql.internal.constraint.DbTableCheckExpression;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Smt2Writer {
    public static final String CHECK_INT_COMPARE_REGEX = "^CHECK\\(([a-zA-Z_][a-zA-Z0-9_]+)([<|>|=]=?)(.+)\\)$";

    // The value that solves the constraint
    List<String> values = new ArrayList<>();

    // The assertions that those values need to satisfy
    List<String> constraints = new ArrayList<>();

    /**
     * Tries to parse the constraint from the DBConstraint, if succeeds returns true
     */
    public boolean addConstraint(DbTableCheckExpression constraint) {
        try {
            String expression = constraint.getSqlCheckExpression().trim().replaceAll(" ", "");

            final Matcher matcher = getCheckMatcher(expression);

            values.add(getValueFromExpression(matcher));
            constraints.add(getConstraintFromExpressionAsText(matcher));

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getValueFromExpression(Matcher matcher) {
        return matcher.group(1);
    }

    private String getConstraintFromExpressionAsText(Matcher matcher) {
        String value = matcher.group(1);
        String comparator = matcher.group(2);
        String compare = matcher.group(3);

        return "(" + comparator + " " + value + " " + compare + ")";
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
        for (String value: values) {
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
        for (String value : values) {
            sb.append("(get-value (");
            sb.append(value);
            sb.append("))\n");
        }
    }

}
