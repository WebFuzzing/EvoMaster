package org.evomaster.client.java.controller.opensearch;

import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import org.evomaster.client.java.controller.opensearch.operations.ComparisonOperation;
import org.evomaster.client.java.controller.opensearch.operations.TermOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.sql.internal.TaintHandler;

public class OpenSearchHeuristicsCalculator {
    private final TaintHandler taintHandler;

    public OpenSearchHeuristicsCalculator() {
        this(null);
    }

    public OpenSearchHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    public double computeExpression(Object query, Object doc) {
        QueryOperation operation = getOperation(query);
        return calculateDistance(operation, doc);
    }

    private QueryOperation getOperation(Object query) {
        return new OpenSearchQueryParser().parse(query);
    }

    private double calculateDistance(QueryOperation operation, Object doc) {
        if (operation instanceof TermOperation<?>) {
            return calculateDistanceForEquals((TermOperation<?>) operation, doc);
        }

        return Double.MAX_VALUE;
    }

    private double calculateDistanceForEquals(TermOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, (Math::abs));
    }

    private double calculateDistanceForComparisonOperation(ComparisonOperation<?> operation, Object doc, DoubleUnaryOperator calculateDistance) {
        Object expectedValue = operation.getValue();
        String field = operation.getFieldName();

        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }

        Object actualValue = ((Map<?,?>) doc).get(field);
        double dif = compareValues(actualValue, expectedValue);

        return calculateDistance.applyAsDouble(dif);
    }

    private double compareValues(Object val1, Object val2) {

        if (val1 instanceof Number && val2 instanceof Number) {
            double x = ((Number) val1).doubleValue();
            double y = ((Number) val2).doubleValue();
            return x - y;
        }

        if (val1 instanceof String && val2 instanceof String) {

            if(taintHandler!=null){
                taintHandler.handleTaintForStringEquals((String)val1,(String)val2, false);
            }

            return (double) DistanceHelper.getLeftAlignmentDistance((String) val1, (String) val2);
        }

        if (val1 instanceof Boolean && val2 instanceof Boolean) {
            return val1 == val2 ? 0d : 1d;
        }

        if (val1 instanceof String && isObjectId(val2)) {
            if(taintHandler!=null){
                taintHandler.handleTaintForStringEquals((String)val1,val2.toString(),false);
            }
            return (double) DistanceHelper.getLeftAlignmentDistance((String) val1, val2.toString());
        }

        if (val2 instanceof String && isObjectId(val1)) {
            if(taintHandler!=null){
                taintHandler.handleTaintForStringEquals(val1.toString(),val2.toString(),false);
            }
            return (double) DistanceHelper.getLeftAlignmentDistance(val1.toString(), (String) val2);
        }

        if (isObjectId(val2) && isObjectId(val1)) {
            return (double) DistanceHelper.getLeftAlignmentDistance(val1.toString(), val2.toString());
        }


        if (val1 instanceof List<?> && val2 instanceof List<?>) {
            // Modify
            return Double.MAX_VALUE;
        }

        return Double.MAX_VALUE;
    }

    private static boolean isObjectId(Object obj) {
        return obj.getClass().getName().equals("org.bson.types.ObjectId");
    }

}
