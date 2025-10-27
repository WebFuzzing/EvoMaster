package org.evomaster.solver.smtlib;

import org.evomaster.solver.smtlib.value.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SMTResultParser class is responsible for parsing responses from the Z3 solver.
 * It converts the raw SMT-LIB response into a map of variable names and their corresponding values.
 */
public class SMTResultParser {

    /**
     * Parses the Z3 solver response and extracts variable values.
     *
     * @param z3Response the raw response from Z3 solver
     * @return a map where keys are variable names and values are SMTLibValue objects representing the parsed values
     */
    public static Map<String, SMTLibValue> parseZ3Response(String z3Response) {
        Map<String, SMTLibValue> results = new HashMap<>();

        // Regular expression for matching simple value assignments, including negative numbers
        // example: (id_1 2), (x (- 4)), or (name_1 "example")
        String simpleValuePattern = "(- )?\\d+|\"[^\"]*\"|[+-]?([0-9]*[.])?[0-9]+";
        // Pattern for matching value assignments
        // example: ((variableName value)) where value can be an integer, string, or real number
        Pattern valuePattern = Pattern.compile("\\(\\((\\w+) \\(?(" + simpleValuePattern + ")\\)?\\)\\)");

        // Regular expression for matching composed types or structures
        // example: ((variableName (field1 value1 field2 value2 ...)))
        String composedValuePattern = "\\(([^)]+)\\)";
        // Pattern for matching composed types with fields
        // example: ((variableName (field1 value1 field2 value2 ...)))
        Pattern composedTypePattern = Pattern.compile("\\(\\((\\w+\\d+) " + composedValuePattern + "\\)\\)");

        // Buffer for multiline values
        StringBuilder buffer = new StringBuilder();

        // Split the Z3 response into individual lines for processing
        String[] lines = z3Response.split("\n");

        for (String line : lines) {
            if (line.startsWith("unsat")) {
                throw new RuntimeException("Unsatisfiable problem");
            }
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }

            if (line.startsWith("sat")) {
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
                String[] fields = composedMatcher.group(2).split(" ");
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
        return results; // Return the map of parsed results
    }

    /**
     * Parses a value string into an appropriate SMTLibValue object.
     *
     * @param value the string value to be parsed
     * @return an SMTLibValue representing the parsed value
     */
    private static SMTLibValue parseValue(String value) {
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
