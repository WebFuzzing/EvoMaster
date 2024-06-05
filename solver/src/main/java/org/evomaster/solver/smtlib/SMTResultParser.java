package org.evomaster.solver.smtlib;


import org.evomaster.solver.smtlib.value.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMTResultParser {

    public static Map<String, SMTLibValue> parseZ3Response(String z3Response) {
        Map<String, SMTLibValue> results = new HashMap<>();

        // Pattern for matching simple value assignments, like (id_1 2) or (name_1 "example")
        String simpleValuePattern = "\\d+|\"[^\"]*\"|[+-]?([0-9]*[.])?[0-9]+";
        // Matches ((variableName value)), where value can be an integer, string, or real number
        Pattern valuePattern = Pattern.compile("\\(\\((\\w+) (" + simpleValuePattern + ")\\)\\)");

        // Pattern for matching composed types or structures, like ((user1 (id 1 name "John" age 30)))
        String composedValuePattern = "\\(([^)]+)\\)";
        // Matches ((variableName (field1 value1 field2 value2 ...)))
        Pattern composedTypePattern = Pattern.compile("\\(\\((\\w+\\d+) " + composedValuePattern + "\\)\\)");

        String[] lines = z3Response.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }

            Matcher composedMatcher = composedTypePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (composedMatcher.find()) {
                String variableName = composedMatcher.group(1);
                String[] fields = composedMatcher.group(2).split(" ");
                String[] fieldNames = fields[0].split("-");

                Map<String, SMTLibValue> structValues = new HashMap<>();
                for (int i = 1; i < fields.length; i++) {
                    String fieldName = fieldNames[i-1].toUpperCase();
                    String value = fields[i];
                    structValues.put(fieldName, parseValue(value));
                }

                results.put(variableName, new StructValue(structValues));
            } else if (valueMatcher.find()) {
                String variableName = valueMatcher.group(1);
                String value = valueMatcher.group(2);
                results.put(variableName, parseValue(value));
            }
        }

        return results;
    }

    private static SMTLibValue parseValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return new StringValue(value.substring(1, value.length() - 1)); // Remove quotes for strings
        }
        try {
            return new IntValue(Integer.parseInt(value)); // Try to parse as integer
        } catch (NumberFormatException e) {
            try {
                return new RealValue(Double.parseDouble(value)); // Try to parse as double (Real)
            } catch (NumberFormatException ex) {
                // Not an integer or real number, return as string
                return new StringValue(value);
            }
        }
    }
}
