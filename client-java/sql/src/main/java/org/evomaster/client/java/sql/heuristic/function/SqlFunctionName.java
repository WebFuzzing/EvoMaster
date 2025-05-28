package org.evomaster.client.java.sql.heuristic.function;

import java.util.Objects;

public class SqlFunctionName implements Comparable<SqlFunctionName> {


    private final String functionName;

    public SqlFunctionName(String functionName) {
        Objects.requireNonNull(functionName);
        this.functionName = functionName.toLowerCase();
    }

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
