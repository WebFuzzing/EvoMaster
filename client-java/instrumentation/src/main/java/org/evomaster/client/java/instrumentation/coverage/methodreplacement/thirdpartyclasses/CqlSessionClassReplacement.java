package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.ExecutedCqlCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CqlSessionClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final CqlSessionClassReplacement singleton = new CqlSessionClassReplacement();

    public static final String CASSANDRA_FIND_STRING_SYNC = "cassandraExecuteStringSync";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.datastax.oss.driver.api.core.CqlSession";
    }

    @Replacement(type = ReplacementType.TRACKER, id = CASSANDRA_FIND_STRING_SYNC, usageFilter = UsageFilter.ANY, category = ReplacementCategory.CASSANDRA, castTo = "com.datastax.oss.driver.api.core.cql.ResultSet")
    public static Object execute(Object cqlSession, String query) {
        return handleCqlExecute(CASSANDRA_FIND_STRING_SYNC, cqlSession, query);
    }

    private static Object handleCqlExecute(String id, Object cqlSession, String query) {
        long start = System.currentTimeMillis();
        try {
            Method executeMethod = retrieveExecuteMethod(id, cqlSession);
            Object result = executeMethod.invoke(cqlSession, query);
            long end = System.currentTimeMillis();
            long executionTime = end - start;
            ExecutedCqlCommand info = new ExecutedCqlCommand(query, false, executionTime);
            ExecutionTracer.addCqlInfo(info);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static Method retrieveExecuteMethod(String id, Object cqlSession){
        return getOriginal(singleton, id, cqlSession);
    }

}