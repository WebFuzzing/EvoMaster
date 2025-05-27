package org.evomaster.client.java.sql.heuristic.function;

import java.util.Map;
import java.util.TreeMap;

public class FunctionFinder {

    private final Map<SqlFunctionName, SqlFunction> sqlFunctions = new TreeMap<>();

    public void addFunction(SqlFunction sqlFunction) {
        if (sqlFunctions.containsKey(sqlFunction.getFunctionName())) {
            throw new IllegalArgumentException("Function " + sqlFunction.getFunctionName() + " already defined");
        }
        sqlFunctions.put(sqlFunction.getFunctionName(), sqlFunction);
    }

    private static FunctionFinder instance = null;

    public static FunctionFinder getInstance() {
        if (instance == null) {
            instance = new FunctionFinder();
        }
        return instance;
    }

    private FunctionFinder() {
        super();
        this.addFunction(new TimeFunction());
        this.addFunction(new UpperFunction());
        this.addFunction(new StringDecodeFunction());
        // aggregation functions
        this.addFunction(new SqlCountFunction());
        this.addFunction(new SqlMaxFunction());
        this.addFunction(new SqlMinFunction());
        this.addFunction(new SqlSumFunction());
        this.addFunction(new SqlAvgFunction());
    }

    public SqlFunction getFunction(String functionName) {
        return sqlFunctions.get(new SqlFunctionName(functionName));
    }

    public boolean containsFunction(String functionName) {
        return sqlFunctions.containsKey(new SqlFunctionName(functionName));
    }
}
