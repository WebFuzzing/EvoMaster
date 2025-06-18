package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.evomaster.client.java.instrumentation.OpenSearchCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

/**
 * Class responsible to handle method calls to the OpenSearchClient and intercepting
 * them to include its information (including operation and arguments) on the ExecutionTracer.
 */
public class OpenSearchClientClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final OpenSearchClientClassReplacement singleton = new OpenSearchClientClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.opensearch.client.opensearch.OpenSearchClient";
    }

    @Replacement(type = ReplacementType.TRACKER, id = "get", usageFilter = UsageFilter.ANY, category = ReplacementCategory.OPENSEARCH, castTo = "org.opensearch.client.opensearch.core.GetResponse")
    public static <TDocument> Object get(Object openSearchClient, @ThirdPartyCast(actualType = "org.opensearch.client.opensearch.core.GetRequest") Object request, Class<TDocument> documentClass) {
        return handleMethod(openSearchClient, "get", Arrays.asList(request, documentClass), request);
    }

    @Replacement(type = ReplacementType.TRACKER, id = "search", usageFilter = UsageFilter.ANY, category = ReplacementCategory.OPENSEARCH, castTo = "org.opensearch.client.opensearch.core.SearchResponse")
    public static <TDocument> Object search(Object openSearchClient, @ThirdPartyCast(actualType = "org.opensearch.client.opensearch.core.SearchRequest") Object request, Class<TDocument> documentClass) {
        return handleMethod(openSearchClient, "search", Arrays.asList(request, documentClass), request);
    }

    /**
     * Executes the method to be replaced adding timing information and inserting the information into the ExecutionTracer
     *
     * @param openSearchClient The OpenSearchClient class object
     * @param method           The method to be executed from the OpenSearchClient
     * @param args             Arguments included on the original method execution
     * @param query            The query argument
     * @return Result of executing the original method with its original arguments
     */
    private static Object handleMethod(Object openSearchClient, String method, List<Object> args, Object query) {
        try {
            long start = System.currentTimeMillis();
            Method clientMethod = retrieveMethod(openSearchClient, method);
            Object result = clientMethod.invoke(openSearchClient, args.toArray());
            long end = System.currentTimeMillis();

            addOpenSearchInfo(method, query, end - start);

            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static Method retrieveMethod(Object openSearchClient, String id) {
        return getOriginal(singleton, id, openSearchClient);
    }

    private static void addOpenSearchInfo(String method, Object query, long executionTime) {
        OpenSearchCommand info = new OpenSearchCommand(getIndex(query), method, query, executionTime);
        ExecutionTracer.addOpenSearchInfo(info);
    }

    private static Object getIndex(Object query) {
        try {
            return query.getClass().getMethod("index").invoke(query);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
