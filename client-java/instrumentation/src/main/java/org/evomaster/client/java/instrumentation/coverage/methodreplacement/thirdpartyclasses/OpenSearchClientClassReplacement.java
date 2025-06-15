package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.evomaster.client.java.instrumentation.OpenSearchFindCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

public class OpenSearchClientClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final OpenSearchClientClassReplacement singleton = new OpenSearchClientClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.opensearch.client.opensearch.OpenSearchClient";
    }

    @Replacement(
        type = ReplacementType.TRACKER,
        id = "get",
        usageFilter = UsageFilter.ANY,
        category = ReplacementCategory.OPENSEARCH,
        castTo = "org.opensearch.client.opensearch.core.GetResponse"
    )
    public static <TDocument> Object get(
        Object openSearchClient,
        @ThirdPartyCast(actualType = "org.opensearch.client.opensearch.core.GetRequest") Object request,
        Class<TDocument> tDocumentClass
    ) {
        return handleFind("get", openSearchClient, Arrays.asList(request, tDocumentClass), request);
    }

    private static Object handleFind(String id, Object openSearchClient, List<Object> args, Object query) {
        long start = System.currentTimeMillis();
        try {
            Method findMethod = retrieveFindMethod(id, openSearchClient);
            Object result = findMethod.invoke(openSearchClient, args.toArray());
            long end = System.currentTimeMillis();
            handleOpenSearch(openSearchClient, query, end - start);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static Method retrieveFindMethod(String id, Object openSearchClient) {
        return getOriginal(singleton, id, openSearchClient);
    }

    // TODO MIGUE: Successfully executed is always true for now, as we do not track exceptions
    private static void handleOpenSearch(Object openSearchClient, Object query, long executionTime) {
        // TODO MIGUE: Set string schema
        //  String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(extractDocumentsType(openSearchClient));

        String schema = "example schema";
        OpenSearchFindCommand
            info = new OpenSearchFindCommand(getIndexName(openSearchClient), schema, query, true, executionTime);
        ExecutionTracer.addOpenSearchInfo(info);
    }

//    private static Class<?> extractDocumentsType(Object openSearchClient) {
//        try {
//            Method indices = getOriginal(singleton, "indices", openSearchClient);
//            Object indicesObject = indices.invoke(openSearchClient);
//
//            Method getFieldMapping = indicesObject.getClass().getMethod("getFieldMapping");
//            Object fieldMapping = getFieldMapping.invoke(indicesObject);
//
//            return fieldMapping;
//        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
//                 IllegalAccessException e) {
//            throw new RuntimeException("Failed to retrieve document's type from index", e);
//        }
//    }

    // TODO-MIGUE: Replace with actual logic to retrieve index name
    private static String getIndexName(Object openSearchClient) {
        return "default-index";
    }


//BulkResponse bulk(BulkRequest request)
//<TDocument> CreateResponse create(CreateRequest<TDocument> request)

//<TDocument> IndexResponse index(IndexRequest<TDocument> request)
//<TDocument> SearchResponse<TDocument> search(SearchRequest request, Class<TDocument> tDocumentClass)
//<TDocument> SearchTemplateResponse<TDocument> searchTemplate(SearchTemplateRequest request, Class<TDocument> tDocumentClass)
//<TDocument> ScrollResponse<TDocument> scroll(ScrollRequest request, Class<TDocument> tDocumentClass)

//<TDocument, TPartialDocument> UpdateResponse<TDocument> update( UpdateRequest<TDocument, TPartialDocument> request, Class<TDocument> tDocumentClass)

//<TDocument> MgetResponse<TDocument> mget(MgetRequest request, Class<TDocument> tDocumentClass)
//<TDocument> MsearchResponse<TDocument> msearch(MsearchRequest request, Class<TDocument> tDocumentClass)
//<TDocument> MsearchTemplateResponse<TDocument> msearchTemplate(MsearchTemplateRequest request, Class<TDocument> tDocumentClass)

//<TDocument> TermvectorsResponse termvectors(TermvectorsRequest<TDocument> request)
//<TResult> ScriptsPainlessExecuteResponse<TResult> scriptsPainlessExecute(ScriptsPainlessExecuteRequest request, Class<TResult> tResultClass)
//<TDocument> GetSourceResponse<TDocument> getSource(GetSourceRequest request, Class<TDocument> tDocumentClass)
//<TDocument> ExplainResponse<TDocument> explain(ExplainRequest request, Class<TDocument> tDocumentClass)
}
