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

        // Regular expression for matching simple value assignments
        // example: (id_1 2) or (name_1 "example")
        String simpleValuePattern = "\\d+|\"[^\"]*\"|[+-]?([0-9]*[.])?[0-9]+";
        // Pattern for matching value assignments
        // example: ((variableName value)) where value can be an integer, string, or real number
        Pattern valuePattern = Pattern.compile("\\(\\((\\w+) (" + simpleValuePattern + ")\\)\\)");

        // Regular expression for matching composed types or structures
        // example: ((variableName (field1 value1 field2 value2 ...)))
        String composedValuePattern = "\\(([^)]+)\\)";
        // Pattern for matching composed types with fields
        // example: ((variableName (field1 value1 field2 value2 ...)))
        Pattern composedTypePattern = Pattern.compile("\\(\\((\\w+\\d+) " + composedValuePattern + "\\)\\)");

        // Split the Z3 response into individual lines for processing
        String[] lines = z3Response.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }

            Matcher composedMatcher = composedTypePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            // Check if the line matches a composed type (structure)
            if (composedMatcher.find()) {
                String variableName = composedMatcher.group(1);
                String[] fields = composedMatcher.group(2).split(" ");
                String[] fieldNames = fields[0].split("-");

                Map<String, SMTLibValue> structValues = new HashMap<>();
                // Iterate over fields to extract field names and values
                for (int i = 1; i < fields.length; i++) {
                    String fieldName = fieldNames[i-1].toUpperCase(); // Use uppercase for field names
                    String value = fields[i];
                    structValues.put(fieldName, parseValue(value)); // Parse and add the field value
                }

                results.put(variableName, new StructValue(structValues)); // Store composed type result
            }
            // Check if the line matches a simple value assignment
            else if (valueMatcher.find()) {
                String variableName = valueMatcher.group(1);
                String value = valueMatcher.group(2);
                results.put(variableName, parseValue(value)); // Parse and store the simple value
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
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // If it is a string
            return new StringValue(value.substring(1, value.length() - 1)); // Remove quotes
        }
        try {
            // Try to parse the value as an integer
            return new IntValue(Integer.parseInt(value));
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
