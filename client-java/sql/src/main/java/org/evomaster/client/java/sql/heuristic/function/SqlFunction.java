package org.evomaster.client.java.sql.heuristic.function;

import java.util.Objects;

public abstract class SqlFunction {

    private final SqlFunctionName sqlFunctionName;

    public SqlFunction(String functionName) {
        Objects.requireNonNull(functionName, "Function name cannot be null");

        this.sqlFunctionName = new SqlFunctionName(functionName   );
    }

    public abstract Object evaluate(Object... arguments);

    public SqlFunctionName getFunctionName() {
        return sqlFunctionName;
    }
}
