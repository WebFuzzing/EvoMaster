package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
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

    private static final String THIRD_PARTY_CLASS = "org.opensearch.client.opensearch.OpenSearchClient";
    private static final String GET_METHOD = "get";
    private static final String SEARCH_METHOD = "search";
    private static final String INDEX_METHOD = "index";
    private static final String GET_QUERY_METHOD = "query";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return THIRD_PARTY_CLASS;
    }

    @Replacement(type = ReplacementType.TRACKER, id = GET_METHOD, usageFilter = UsageFilter.ANY, category = ReplacementCategory.OPENSEARCH, castTo = "org.opensearch.client.opensearch.core.GetResponse")
    public static <T> Object get(Object openSearchClient, @ThirdPartyCast(actualType = "org.opensearch.client.opensearch.core.GetRequest") Object request, Class<T> documentClass) {
        return handleMethod(openSearchClient, GET_METHOD, Arrays.asList(request, documentClass), request);
    }

    @Replacement(type = ReplacementType.TRACKER, id = SEARCH_METHOD, usageFilter = UsageFilter.ANY, category = ReplacementCategory.OPENSEARCH, castTo = "org.opensearch.client.opensearch.core.SearchResponse")
    public static <T> Object search(Object openSearchClient, @ThirdPartyCast(actualType = "org.opensearch.client.opensearch.core.SearchRequest") Object request, Class<T> documentClass) {
        return handleMethod(openSearchClient, SEARCH_METHOD, Arrays.asList(request, documentClass), request);
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
        OpenSearchCommand info = new OpenSearchCommand(getIndex(query), method, getQuery(query), executionTime);
        ExecutionTracer.addOpenSearchInfo(info);
    }

    private static Object getQuery(Object query) {
        try {
            Object result =  query.getClass().getMethod(GET_QUERY_METHOD).invoke(query);
            if (result == null) {
                return null;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    private static List<String> getIndex(Object query) {
        try {
            Object result = query.getClass().getMethod(INDEX_METHOD).invoke(query);
            if (result == null) {
                return null;
            }

            if (result instanceof List) {
                List<String> indexList = (List<String>) result;
                return indexList;
            } else if (result instanceof String) {
                return Collections.singletonList((String) result);
            } else {
                return Collections.singletonList(result.toString());
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
