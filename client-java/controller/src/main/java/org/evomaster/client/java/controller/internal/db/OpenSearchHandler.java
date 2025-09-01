package org.evomaster.client.java.controller.internal.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.evomaster.client.java.controller.opensearch.OpenSearchHeuristicsCalculator;
import org.evomaster.client.java.instrumentation.OpenSearchCommand;
import org.evomaster.client.java.utils.SimpleLogger;

public class OpenSearchHandler {

    public static final String OPENSEARCH_CLIENT_CLASS_NAME = "org.opensearch.client.opensearch.OpenSearchClient";
    public static final String OPENSEARCH_CLIENT_INDEX_METHOD_NAME = "index";
    public static final String OPENSEARCH_CLIENT_SIZE_METHOD_NAME = "size";
    public static final String OPENSEARCH_CLIENT_BUILD_METHOD_NAME = "build";
    public static final String OPENSEARCH_CLIENT_SEARCH_METHOD_NAME = "search";
    public static final String OPENSEARCH_CLIENT_HITS_METHOD_NAME = "hits";

    public static final String OPENSEARCH_CLIENT_SEARCH_REQUEST_CLASS_NAME = "org.opensearch.client.opensearch.core.SearchRequest";
    public static final String OPENSEARCH_CLIENT_SEARCH_REQUEST_BUILDER_CLASS_NAME = "org.opensearch.client.opensearch.core.SearchRequest$Builder";

    private final List<OpenSearchCommand> commands;

    private final boolean calculateHeuristics;

    /**
     * The heuristics based on the OpenSearch execution
     */
    private final List<OpenSearchCommandWithDistance> commandsWithDistances;

    private final OpenSearchHeuristicsCalculator calculator = new OpenSearchHeuristicsCalculator();

    private Object openSearchClient = null;

    public OpenSearchHandler() {
        this.commands = new ArrayList<>();
        this.commandsWithDistances = new ArrayList<>();
        this.calculateHeuristics = true;
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public void handle(OpenSearchCommand command) {
        commands.add(command);
    }

    public List<OpenSearchCommandWithDistance> getEvaluatedOpenSearchCommands() {
        commands.stream()
            .filter(command -> command.getQuery() != null)
            .forEach(openSearchCommand -> {
                OpenSearchDistanceWithMetrics distanceWithMetrics = computeCommandDistance(openSearchCommand);
                commandsWithDistances.add(new OpenSearchCommandWithDistance(openSearchCommand, distanceWithMetrics));
        });

        commands.clear();

        return commandsWithDistances;
    }

    private OpenSearchDistanceWithMetrics computeCommandDistance(OpenSearchCommand command) {
        List<String> indexName = command.getIndex();
        List<?> documents = getDocuments(indexName);

        double min = Double.MAX_VALUE;
        int numberOfEvaluatedDocuments = 0;
        for (Object doc : documents) {
            numberOfEvaluatedDocuments += 1;
            double findDistance;
            try {
                findDistance = calculator.computeExpression(command.getQuery(), doc);
            } catch (Exception ex) {
                SimpleLogger.uniqueWarn("Failed to compute find: " + command.getQuery() + " with data " + doc);
                findDistance = Double.MAX_VALUE;
            }

            if (findDistance == 0) {
                return new OpenSearchDistanceWithMetrics(0, numberOfEvaluatedDocuments);
            } else if (findDistance < min) {
                min = findDistance;
            }
        }

        return new OpenSearchDistanceWithMetrics(min, numberOfEvaluatedDocuments);
    }

    private List<Object> getDocuments(List<String> indexNames) {
        try {
            // Get the OpenSearchClient class
            Class<?> openSearchClientClass = openSearchClient.getClass();

            // Get SearchRequest.Builder class
            Class<?> searchRequestBuilderClass = Class.forName(OPENSEARCH_CLIENT_SEARCH_REQUEST_BUILDER_CLASS_NAME);
            Object searchRequestBuilder = searchRequestBuilderClass.getDeclaredConstructor().newInstance();

            // Set the index/indices on the builder
            Method indexMethod = searchRequestBuilderClass.getMethod(OPENSEARCH_CLIENT_INDEX_METHOD_NAME, List.class);
            indexMethod.invoke(searchRequestBuilder, indexNames);

            // Set a large size to get all documents (could use scroll for large datasets)
            Method sizeMethod = searchRequestBuilderClass.getMethod(OPENSEARCH_CLIENT_SIZE_METHOD_NAME, Integer.class);
            sizeMethod.invoke(searchRequestBuilder, 10000); // Max 10k documents

            // Build the SearchRequest
            Method buildMethod = searchRequestBuilderClass.getMethod(OPENSEARCH_CLIENT_BUILD_METHOD_NAME);
            Object searchRequest = buildMethod.invoke(searchRequestBuilder);

            // Execute the search: openSearchClient.search(searchRequest, Object.class)
            Method searchMethod = openSearchClientClass.getMethod(OPENSEARCH_CLIENT_SEARCH_METHOD_NAME,
                Class.forName(OPENSEARCH_CLIENT_SEARCH_REQUEST_CLASS_NAME),
                Class.class);
            Object searchResponse = searchMethod.invoke(openSearchClient, searchRequest, Object.class);

            Method hitsMethod = searchResponse.getClass().getMethod(OPENSEARCH_CLIENT_HITS_METHOD_NAME);
            Object hitsContainer = hitsMethod.invoke(searchResponse);

            Method hitsListMethod = hitsContainer.getClass().getMethod(OPENSEARCH_CLIENT_HITS_METHOD_NAME);
            Object hitsList = hitsListMethod.invoke(hitsContainer);

            // loop hitslist as if it was a list
            List<Object> result = new ArrayList<>();
            for (Object hit : (List<Object>)hitsList) {
                Method hitSourceMethod = hit.getClass().getMethod("source");
                result.add(hitSourceMethod.invoke(hit));
            }

            return result; // Returns List<Document>

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve documents from OpenSearch indices: " + indexNames, e);
        }
    }

    private Object getDocuments() {
        // Default implementation - this method should be called with specific index names
        return null;
    }

    public void setOpenSearchClient(Object openSearchClient) {
        this.openSearchClient = openSearchClient;
    }
}
