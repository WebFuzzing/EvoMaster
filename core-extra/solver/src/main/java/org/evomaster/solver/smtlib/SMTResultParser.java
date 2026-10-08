package org.evomaster.solver.smtlib;

import org.evomaster.solver.Z3Solution;
import org.evomaster.solver.smtlib.value.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SMTResultParser class is responsible for parsing responses from the Z3 solver.
 * It converts the raw SMT-LIB response into a {@link Z3Solution} of variable names and
 * their corresponding values.
 */
public class SMTResultParser {

    /**
     * Parses the Z3 solver response and extracts variable values.
     *
     * FRAGILITY: parsing is regex- and position-based and assumes Z3's textual get-value layout
     * (constructor name split on '-', values split on whitespace outside strings and parentheses).
     * It therefore assumes column names contain no hyphens.
     *
     * @param z3Response the raw response from Z3 solver
     * @return a {@link Z3Solution} mapping variable names to the {@link SMTLibValue} objects Z3 assigned to them
     */
    public static Z3Solution parseZ3Response(String z3Response) {
        Map<String, SMTLibValue> results = new HashMap<>();

        // Regular expression for matching simple value assignments, including negative numbers
        // example: (id_1 2), (x (- 4)), or (name_1 "example")
        String simpleValuePattern = "(- )?\\d+|\"[^\"]*\"|[+-]?([0-9]*[.])?[0-9]+";
        // Pattern for matching value assignments
        // example: ((variableName value)) where value can be an integer, string, or real number
        Pattern valuePattern = Pattern.compile("\\(\\((\\w+) \\(?(" + simpleValuePattern + ")\\)?\\)\\)");

        // Pattern for matching the start of a composed type (structure); its body is read by structTokens
        // example: ((variableName (field1-field2-... value1 value2 ...)))
        // The constructor name starts with a letter, which tells it apart from a value such as (- 4)
        Pattern composedTypePattern = Pattern.compile("\\(\\((\\w+\\d+) \\((?=[A-Za-z_])");

        // Buffer for multiline values
        StringBuilder buffer = new StringBuilder();

        // Split the Z3 response into individual lines for processing
        String[] lines = z3Response.split("\n");

        for (String line : lines) {
            // Defensive: Z3DockerExecutor already classifies 'unsat'/'unknown' before invoking this
            // parser, so in practice only 'sat' models reach here. This guard is kept as a safety net
            // in case the parser is ever called directly with an unsat response.
            if (line.startsWith(CheckSatResponse.UNSAT)) {
                throw new RuntimeException("Unsatisfiable problem");
            }
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }

            if (line.startsWith(CheckSatResponse.SAT)) {
                buffer.setLength(0); // Reset buffer if a new result starts
                continue;
            }

            String sanitizedLine = line.trim().replaceAll("\\(-\\s+(\\d+)\\)", "(-$1)"); // Remove spaces for negative numbers, example: (- 321)
            buffer.append(sanitizedLine).append(" ");

            // Check if the buffer contains a complete expression
            Matcher composedMatcher = composedTypePattern.matcher(buffer.toString());
            Matcher valueMatcher = valuePattern.matcher(buffer.toString());

            // Check if the buffer matches a composed type (structure)
            if (composedMatcher.find()) {
                String variableName = composedMatcher.group(1);
                /*
                    Values must not be split on every space: a string value containing a space or a
                    parenthesis -- e.g. the literal of a WHERE name = 'John Smith' -- would shift every
                    later field and run past the end of fieldNames.
                 */
                List<String> structTokens = structTokens(buffer.toString(), composedMatcher.end());
                if (structTokens == null) {
                    continue; // the struct is not complete yet: keep buffering lines
                }
                String[] fields = structTokens.toArray(new String[0]);
                String[] fieldNames = fields[0].split("-");

                Map<String, SMTLibValue> structValues = new HashMap<>();
                // Iterate over fields to extract field names and values
                for (int i = 1; i < fields.length; i++) {
                    String fieldName = fieldNames[i - 1].toUpperCase(); // Use uppercase for field names
                    String value = fields[i];
                    structValues.put(fieldName, parseValue(value)); // Parse and add the field value
                }

                results.put(variableName, new StructValue(structValues)); // Store composed type result
                buffer.setLength(0); // Clear the buffer after processing
            }
            // Check if the buffer matches a simple value assignment
            else if (valueMatcher.find()) {
                String variableName = valueMatcher.group(1);
                String value = valueMatcher.group(2);
                // Handle cases where the value is wrapped in parentheses, like (- 4)
                if (value.startsWith("(") && value.endsWith(")")) {
                    value = value.substring(1, value.length() - 1).trim(); // Remove parentheses
                }
                results.put(variableName, parseValue(value)); // Parse and store the simple value
                buffer.setLength(0); // Clear the buffer after processing
            }
        }
        return new Z3Solution(results); // Return the parsed assignments as a solution
    }

    /**
     * Splits the body of a struct into its top-level tokens: the constructor name followed by one token
     * per field value.
     *
     * Whitespace separates tokens only outside string literals and outside nested parentheses, so a
     * string such as {@code "John Smith"} or a negative number such as {@code (- 4)} stays a single
     * token. Inside a string, SMT-LIB writes a double quote as two double quotes, which therefore does
     * not end the literal.
     *
     * @param text  the buffered response
     * @param start index of the first character of the struct body, just after its opening parenthesis
     * @return the tokens, or null when the closing parenthesis of the struct has not been read yet
     */
    private static List<String> structTokens(String text, int start) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                current.append(c);
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else if (c == '"') {
                inString = true;
                current.append(c);
            } else if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                if (depth == 0) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                    }
                    return tokens;
                }
                depth--;
                current.append(c);
            } else if (Character.isWhitespace(c) && depth == 0) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        return null;
    }

    /**
     * Evaluates a numeric term as Z3 prints it in a model: a literal ({@code 6}, {@code 20.0}), a
     * negation ({@code (- 6)}, {@code (-6)}, {@code (- 20.0)}) or, for a Real that is not an integer,
     * a division of two literals ({@code (/ 1999.0 100.0)}, {@code (- (/ 7.0 2.0))}).
     *
     * @param term the trimmed value token
     * @return the value, or null when the term is not numeric
     */
    private static SMTLibValue parseNumericTerm(String term) {
        if (term.startsWith("(") && term.endsWith(")")) {
            String inner = term.substring(1, term.length() - 1).trim();
            if (inner.startsWith("-")) {
                SMTLibValue operand = parseNumericTerm(inner.substring(1).trim());
                if (operand instanceof LongValue) {
                    return new LongValue(-((LongValue) operand).getValue());
                }
                if (operand instanceof RealValue) {
                    return new RealValue(-((RealValue) operand).getValue());
                }
                return null;
            }
            if (inner.startsWith("/")) {
                String[] operands = inner.substring(1).trim().split("\\s+");
                if (operands.length != 2) {
                    return null;
                }
                SMTLibValue numerator = parseNumericTerm(operands[0]);
                SMTLibValue denominator = parseNumericTerm(operands[1]);
                if (numerator == null || denominator == null) {
                    return null;
                }
                return new RealValue(toDouble(numerator) / toDouble(denominator));
            }
            return null;
        }
        if (term.matches("\\d+")) {
            try {
                return new LongValue(Long.parseLong(term));
            } catch (NumberFormatException e) {
                return new RealValue(Double.parseDouble(term)); // too large for a Long
            }
        }
        if (term.matches("\\d+\\.\\d+")) {
            return new RealValue(Double.parseDouble(term));
        }
        return null;
    }

    private static double toDouble(SMTLibValue number) {
        return number instanceof LongValue
                ? ((LongValue) number).getValue()
                : ((RealValue) number).getValue();
    }

    /**
     * Parses a value string into an appropriate SMTLibValue object.
     *
     * @param value the string value to be parsed
     * @return an SMTLibValue representing the parsed value
     */
    private static SMTLibValue parseValue(String value) {
        SMTLibValue number = parseNumericTerm(value.trim());
        if (number != null) {
            return number;
        }
        if (value.startsWith("(")) {
            // If it is a negative number in parentheses
            value = value.substring(1).trim(); // Remove parentheses
        }
        if (value.endsWith(")")) {
            // If it is a negative number in parentheses
            value = value.substring(0, value.length() - 1).trim(); // Remove parentheses
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // If it is a string
            return new StringValue(value.substring(1, value.length() - 1)); // Remove quotes
        }
        try {
            if (value.matches("- \\d+")) {
                value = value.replaceFirst("- ", "-"); // Remove space after negative sign
            }
            // Try to parse the value as Long
            return new LongValue(Long.parseLong(value));
        } catch (NumberFormatException e) {
            try {
                // Try to parse the value as a real number (double)
                return new RealValue(Double.parseDouble(value));
            } catch (NumberFormatException ex) {
                // If not an integer or real number, treat it as a string
                return new StringValue(value);
            }
        }
    }
}
