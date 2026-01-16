package org.evomaster.client.java.sql.heuristic.function;

import java.util.Objects;

public class SqlFunctionName implements Comparable<SqlFunctionName> {


    /**
     * The name of the SQL function.
     */
    private final String functionName;

    /**
     * Constructs a new SqlFunctionName instance with the specified function name.
     * The function name is converted to lowercase to ensure consistency.
     *
     * @param functionName the name of the SQL function. Must not be null.
     *                      Throws {@code NullPointerException} if the input is null.
     */
    public SqlFunctionName(String functionName) {
        Objects.requireNonNull(functionName);
        this.functionName = functionName.toLowerCase();
    }

    /**
     * Retrieves the name of the SQL function.
     *
     * @return the name of the SQL function as a string. This value is stored in lowercase.
     */
    public String getFunctionName() {
        return functionName;
    }


    @Override
    public boolean equals(Object o) {
        if (o==null) {
            return false;
        } else {
            if (o instanceof SqlFunctionName) {
                return functionName.equals(((SqlFunctionName) o).getFunctionName());
            } else {
                return false;
            }
        }
    }

    @Override
    public int hashCode() {
        return functionName.hashCode();
    }

    @Override
    public int compareTo(SqlFunctionName o) {
        return this.functionName.compareTo(o.functionName);
    }
}
