package org.evomaster.clientJava.instrumentation.staticState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionTracer {

    /*
        Careful if you change the signature of any of the
        methods in this class, as they are injected in the
        bytecode instrumentation.
        Fortunately, unit tests should quickly find such
        type of issues.
     */

    /**
     * Key -> the unique id of the coverage objective
     * <br>
     * Value -> heuristic [0,1], where 1 means covered
     */
    private static final Map<String, Double> objectiveCoverage =
            new ConcurrentHashMap<>(65536);


    public static void resetState(){
        objectiveCoverage.clear();
    }

    public static Map<String, Double> getInternalReferenceToObjectiveCoverage() {
        return objectiveCoverage;
    }


    public static final String EXECUTED_LINE_METHOD_NAME = "executedLine" ;
    public static final String EXECUTED_LINE_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/String;I)V";
    public static void executedLine(String className, String fullMethodName, int line){

        String id = "Line_"+line+"_at_"+className+"::"+fullMethodName;
        objectiveCoverage.put(id, 1d);
    }
}
