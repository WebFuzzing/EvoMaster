package org.evomaster.client.java.sql.heuristic.function;

public class StringDecodeFunction extends SqlFunction {

    public StringDecodeFunction() {
        super("STRINGDECODE");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length !=1) {
            throw new IllegalArgumentException("STRINGDECODE() function takes exactly one argument but got: " + arguments.length + "");
        }

        Object concreteValue = arguments[0];
        if (concreteValue==null) {
            return null;
        } else if (concreteValue instanceof String) {
            return evaluateStringDecode((String) concreteValue);
        } else {
            throw new IllegalArgumentException("STRINGDECODE() function takes a string argument, but got a " + concreteValue.getClass().getName());
        }
    }

    private String evaluateStringDecode(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'b': sb.append('\b'); break;
                    case 't': sb.append('\t'); break;
                    case 'n': sb.append('\n'); break;
                    case 'f': sb.append('\f'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('\"'); break;
                    case '\'': sb.append('\''); break;
                    case '\\': sb.append('\\'); break;
                    case 'u':
                        if (i + 4 >= s.length()) {
                            throw new IllegalArgumentException("Incomplete Unicode escape: " + s.substring(i));
                        }
                        String hex = s.substring(i + 1, i + 5);
                        try {
                            int code = Integer.parseInt(hex, 16);
                            sb.append((char) code);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid Unicode escape: \\u" + hex);
                        }
                        i += 4;  // skip the next 4 characters
                        break;
                    default:
                        // Unrecognized escape; include as-is
                        sb.append('\\').append(next);
                        break;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

}
