package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.operations.synthetic.*;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static java.lang.Math.abs;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

public class MongoHeuristicsCalculator {

    public static final double MIN_DISTANCE_TO_TRUE_VALUE = 1.0;

    /**
     * Compute a "branch" distance heuristics.
     *
     * @param query the QUERY clause which we want to resolve as true
     * @param doc   a document in the database for which we want to calculate the distance
     * @return a branch distance, where 0 means that the document would make the QUERY resolve as true
     */
    public double computeExpression(Object query, Object doc) {
        QueryOperation operation = getOperation(query);
        return calculateDistance(operation, doc);
    }

    private QueryOperation getOperation(Object query) {
        return new QueryParser().parse(query);
    }

    private double calculateDistance(QueryOperation operation, Object doc) {
        if (operation instanceof EqualsOperation<?>)
            return calculateDistanceForEquals((EqualsOperation<?>) operation, doc);
        if (operation instanceof NotEqualsOperation<?>)
            return calculateDistanceForNotEquals((NotEqualsOperation<?>) operation, doc);
        if (operation instanceof GreaterThanOperation<?>)
            return calculateDistanceForGreaterThan((GreaterThanOperation<?>) operation, doc);
        if (operation instanceof GreaterThanEqualsOperation<?>)
            return calculateDistanceForGreaterEqualsThan((GreaterThanEqualsOperation<?>) operation, doc);
        if (operation instanceof LessThanOperation<?>)
            return calculateDistanceForLessThan((LessThanOperation<?>) operation, doc);
        if (operation instanceof LessThanEqualsOperation<?>)
            return calculateDistanceForLessEqualsThan((LessThanEqualsOperation<?>) operation, doc);
        if (operation instanceof AndOperation) return calculateDistanceForAnd((AndOperation) operation, doc);
        if (operation instanceof OrOperation) return calculateDistanceForOr((OrOperation) operation, doc);
        if (operation instanceof NorOperation) return calculateDistanceForNor((NorOperation) operation, doc);
        if (operation instanceof InOperation<?>) return calculateDistanceForIn((InOperation<?>) operation, doc);
        if (operation instanceof NotInOperation<?>)
            return calculateDistanceForNotIn((NotInOperation<?>) operation, doc);
        if (operation instanceof AllOperation<?>) return calculateDistanceForAll((AllOperation<?>) operation, doc);
        if (operation instanceof InvertedAllOperation<?>)
            return calculateDistanceForInvertedAll((InvertedAllOperation<?>) operation, doc);
        if (operation instanceof SizeOperation) return calculateDistanceForSize((SizeOperation) operation, doc);
        if (operation instanceof InvertedSizeOperation)
            return calculateDistanceForInvertedSize((InvertedSizeOperation) operation, doc);
        if (operation instanceof ElemMatchOperation)
            return calculateDistanceForElemMatch((ElemMatchOperation) operation, doc);
        if (operation instanceof ExistsOperation) return calculateDistanceForExists((ExistsOperation) operation, doc);
        if (operation instanceof ModOperation) return calculateDistanceForMod((ModOperation) operation, doc);
        if (operation instanceof InvertedModOperation)
            return calculateDistanceForInvertedMod((InvertedModOperation) operation, doc);
        if (operation instanceof NotOperation) return calculateDistanceForNot((NotOperation) operation, doc);
        if (operation instanceof TypeOperation) return calculateDistanceForType((TypeOperation) operation, doc);
        if (operation instanceof InvertedTypeOperation)
            return calculateDistanceForInvertedType((InvertedTypeOperation) operation, doc);
        return Double.MAX_VALUE;
    }

    private double calculateDistanceForEquals(EqualsOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, (Math::abs));
    }

    private double calculateDistanceForNotEquals(NotEqualsOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, ((dif) -> dif != 0.0 ? 0.0 : MIN_DISTANCE_TO_TRUE_VALUE));
    }

    private double calculateDistanceForGreaterThan(GreaterThanOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, ((dif) -> dif > 0 ? 0.0 : 1.0 - dif));
    }

    private double calculateDistanceForGreaterEqualsThan(GreaterThanEqualsOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, ((dif) -> dif >= 0 ? 0.0 : -dif));
    }

    private double calculateDistanceForLessThan(LessThanOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, ((dif) -> dif < 0 ? 0.0 : 1.0 + dif));
    }

    private double calculateDistanceForLessEqualsThan(LessThanEqualsOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, ((dif) -> dif <= 0 ? 0.0 : dif));
    }

    private double calculateDistanceForComparisonOperation(ComparisonOperation<?> operation, Object doc, DoubleUnaryOperator calculateDistance) {
        Object expectedValue = operation.getValue();
        String field = operation.getFieldName();

        if (!documentContainsField(doc, field)) {
            return operation instanceof NotEqualsOperation ? 0.0 : Double.MAX_VALUE;
        }

        Object actualValue = getValue(doc, field);
        double dif = compareValues(actualValue, expectedValue);

        return calculateDistance.applyAsDouble(dif);
    }

    private double calculateDistanceForOr(OrOperation operation, Object doc) {
        return operation.getConditions().stream()
                .mapToDouble(condition -> calculateDistance(condition, doc))
                .min()
                .getAsDouble();
    }

    private double calculateDistanceForAnd(AndOperation operation, Object doc) {
        return operation.getConditions().stream().mapToDouble(condition -> calculateDistance(condition, doc)).sum();
    }

    private double calculateDistanceForIn(InOperation<?> operation, Object doc) {
        List<?> expectedValues = operation.getValues();
        Object actualValue = getValue(doc, operation.getFieldName());

        if (actualValue instanceof List<?>) {
            return expectedValues.stream()
                    .mapToDouble(value -> distanceToClosestElem((List<?>) actualValue, value))
                    .min()
                    .getAsDouble();
        } else {
            return distanceToClosestElem(expectedValues, actualValue);
        }
    }

    private double calculateDistanceForNotIn(NotInOperation<?> operation, Object doc) {
        List<?> unexpectedValues = operation.getValues();

        if (!documentContainsField(doc, operation.getFieldName())) return 0.0;

        Object actualValue = getValue(doc, operation.getFieldName());
        boolean hasUnexpectedElement =
                unexpectedValues.stream().anyMatch(value -> compareValues(actualValue, value) == 0.0);

        return hasUnexpectedElement ? MIN_DISTANCE_TO_TRUE_VALUE : 0.0;
    }

    private double calculateDistanceForAll(AllOperation<?> operation, Object doc) {
        List<?> expectedValues = operation.getValues();
        Object actualValues = getValue(doc, operation.getFieldName());

        if (actualValues instanceof Iterable<?>) {
            return expectedValues.stream().mapToDouble(value -> distanceToClosestElem((List<?>) actualValues, value)).sum();
        } else {
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceForInvertedAll(InvertedAllOperation<?> operation, Object doc) {
        List<?> expectedValues = operation.getValues();
        Object actualValues = getValue(doc, operation.getFieldName());

        if (actualValues instanceof List<?>) {
            boolean containsAll = ((List<?>) actualValues).containsAll(expectedValues);
            return containsAll ? MIN_DISTANCE_TO_TRUE_VALUE : 0.0;
        } else {
            return 0.0;
        }
    }

    private double calculateDistanceForSize(SizeOperation operation, Object doc) {
        Integer expectedSize = operation.getValue();
        Object actualValue = getValue(doc, operation.getFieldName());

        if (actualValue instanceof List<?>) {
            Integer actualSize = ((List<?>) actualValue).size();
            return abs(actualSize - expectedSize);
        } else {
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceForInvertedSize(InvertedSizeOperation operation, Object doc) {
        Integer expectedSize = operation.getValue();
        Object actualValue = getValue(doc, operation.getFieldName());

        if (actualValue instanceof List<?>) {
            Integer actualSize = ((List<?>) actualValue).size();
            return actualSize.equals(expectedSize) ? MIN_DISTANCE_TO_TRUE_VALUE : 0.0;
        } else {
            return 0.0;
        }
    }

    private double calculateDistanceForElemMatch(ElemMatchOperation operation, Object doc) {
        Object actualValue = getValue(doc, operation.getFieldName());

        if (actualValue instanceof List<?>) {
            List<?> val = (List<?>) actualValue;
            return val.stream()
                    .mapToDouble(elem -> {
                        Object newDoc = newDocument(doc);
                        appendToDocument(newDoc, operation.getFieldName(), elem);
                        return calculateDistance(operation.getCondition(), newDoc);
                    })
                    .min()
                    .getAsDouble();
        } else {
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceForExists(ExistsOperation operation, Object doc) {
        String expectedField = operation.getFieldName();
        Set<String> actualFields = documentKeys(doc);

        if (operation.getBoolean()) {
            return actualFields.stream()
                    .mapToDouble(field -> DistanceHelper.getLeftAlignmentDistance(field, expectedField))
                    .min()
                    .getAsDouble();
        } else {
            return !documentContainsField(doc, expectedField) ? 0.0 : MIN_DISTANCE_TO_TRUE_VALUE;
        }
    }

    private double calculateDistanceForMod(ModOperation operation, Object doc) {
        Long expectedRemainder = operation.getRemainder();
        Object actualValue = getValue(doc, operation.getFieldName());

        // Change to number?
        if (actualValue instanceof Integer) {
            long actualRemainder = ((Integer) actualValue) % operation.getDivisor();
            return (double) abs(actualRemainder - expectedRemainder);
        } else {
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceForInvertedMod(InvertedModOperation operation, Object doc) {
        Long expectedRemainder = operation.getRemainder();
        Object actualValue = getValue(doc, operation.getFieldName());

        // Change to number?
        if (actualValue instanceof Integer) {
            long actualRemainder = ((Integer) actualValue) % operation.getDivisor();
            return actualRemainder == expectedRemainder ? MIN_DISTANCE_TO_TRUE_VALUE : 0.0;
        } else {
            return 0.0;
        }
    }

    private double calculateDistanceForNot(NotOperation operation, Object doc) {
        String fieldName = operation.getFieldName();
        if (getValue(doc, fieldName) == null) return 0.0;

        QueryOperation condition = operation.getCondition();
        QueryOperation invertedOperation = invertOperation(condition);

        return calculateDistance(invertedOperation, doc);
    }

    private double calculateDistanceForNor(NorOperation operation, Object doc) {
        return operation.getConditions().stream().mapToDouble(condition -> calculateDistance(invertOperation(condition), doc)).sum();
    }

    private double calculateDistanceForType(TypeOperation operation, Object doc) {
        String field = operation.getFieldName();
        String expectedType = getType(operation.getType());
        Object value = getValue(doc, field);
        String actualType = value == null ? "null" : value.getClass().getTypeName();

        return (double) DistanceHelper.getLeftAlignmentDistance(actualType, expectedType);
    }

    private double calculateDistanceForInvertedType(InvertedTypeOperation operation, Object doc) {
        String field = operation.getFieldName();
        String expectedType = getType(operation.getType());
        Object value = getValue(doc, field);
        String actualType = value == null ? null : value.getClass().getTypeName();

        return !Objects.equals(actualType, expectedType) ? 0.0 : MIN_DISTANCE_TO_TRUE_VALUE;
    }

    private QueryOperation invertOperation(QueryOperation operation) {
        if (operation instanceof EqualsOperation<?>) {
            EqualsOperation<?> op = (EqualsOperation<?>) operation;
            return new NotEqualsOperation<>(op.getFieldName(), op.getValue());
        }
        if (operation instanceof NotEqualsOperation<?>) {
            NotEqualsOperation<?> op = (NotEqualsOperation<?>) operation;
            return new EqualsOperation<>(op.getFieldName(), op.getValue());
        }
        if (operation instanceof GreaterThanOperation<?>) {
            GreaterThanOperation<?> op = (GreaterThanOperation<?>) operation;
            return new LessThanEqualsOperation<>(op.getFieldName(), op.getValue());
        }
        if (operation instanceof GreaterThanEqualsOperation<?>) {
            GreaterThanEqualsOperation<?> op = (GreaterThanEqualsOperation<?>) operation;
            return new LessThanOperation<>(op.getFieldName(), op.getValue());
        }
        if (operation instanceof LessThanOperation<?>) {
            LessThanOperation<?> op = (LessThanOperation<?>) operation;
            return new GreaterThanEqualsOperation<>(op.getFieldName(), op.getValue());
        }
        if (operation instanceof LessThanEqualsOperation<?>) {
            LessThanEqualsOperation<?> op = (LessThanEqualsOperation<?>) operation;
            return new GreaterThanOperation<>(op.getFieldName(), op.getValue());
        }
        if (operation instanceof NotOperation) {
            NotOperation op = (NotOperation) operation;
            return op.getCondition();
        }
        if (operation instanceof AllOperation<?>) {
            AllOperation<?> op = (AllOperation<?>) operation;
            return new InvertedAllOperation<>(op.getFieldName(), op.getValues());
        }
        if (operation instanceof InvertedAllOperation<?>) {
            InvertedAllOperation<?> op = (InvertedAllOperation<?>) operation;
            return new AllOperation<>(op.getFieldName(), op.getValues());
        }
        if (operation instanceof AndOperation) {
            AndOperation op = (AndOperation) operation;
            List<QueryOperation> invertedConditions = op.getConditions().stream().map(this::invertOperation).collect(Collectors.toList());
            return new OrOperation(invertedConditions);
        }
        if (operation instanceof OrOperation) {
            OrOperation op = (OrOperation) operation;
            return new NorOperation(op.getConditions());
        }
        if (operation instanceof ExistsOperation) {
            ExistsOperation op = (ExistsOperation) operation;
            return new ExistsOperation(op.getFieldName(), !op.getBoolean());
        }
        if (operation instanceof InOperation<?>) {
            InOperation<?> op = (InOperation<?>) operation;
            return new NotInOperation<>(op.getFieldName(), op.getValues());
        }
        if (operation instanceof NotInOperation<?>) {
            NotInOperation<?> op = (NotInOperation<?>) operation;
            return new InOperation<>(op.getFieldName(), op.getValues());
        }
        if (operation instanceof ModOperation) {
            ModOperation op = (ModOperation) operation;
            return new InvertedModOperation(op.getFieldName(), op.getDivisor(), op.getRemainder());
        }
        if (operation instanceof InvertedModOperation) {
            InvertedModOperation op = (InvertedModOperation) operation;
            return new ModOperation(op.getFieldName(), op.getDivisor(), op.getRemainder());
        }
        if (operation instanceof NorOperation) {
            NorOperation op = (NorOperation) operation;
            return new OrOperation(op.getConditions());
        }
        if (operation instanceof SizeOperation) {
            SizeOperation op = (SizeOperation) operation;
            return new InvertedSizeOperation(op.getFieldName(), op.getValue());
        }
        if (operation instanceof InvertedSizeOperation) {
            InvertedSizeOperation op = (InvertedSizeOperation) operation;
            return new SizeOperation(op.getFieldName(), op.getValue());
        }
        if (operation instanceof TypeOperation) {
            TypeOperation op = (TypeOperation) operation;
            return new InvertedTypeOperation(op.getFieldName(), op.getType());
        }
        if (operation instanceof InvertedTypeOperation) {
            InvertedTypeOperation op = (InvertedTypeOperation) operation;
            return new TypeOperation(op.getFieldName(), op.getType());
        }
        return operation;
    }

    private double compareValues(Object val1, Object val2) {

        if (val1 instanceof Number && val2 instanceof Number) {
            double x = ((Number) val1).doubleValue();
            double y = ((Number) val2).doubleValue();
            return x - y;
        }

        if (val1 instanceof String && val2 instanceof String) {
            return (double) DistanceHelper.getLeftAlignmentDistance((String) val1, (String) val2);
        }

        if (val1 instanceof Boolean && val2 instanceof Boolean) {
            return val1 == val2 ? 0d : 1d;
        }

        if (val1 instanceof List<?> && val2 instanceof List<?>) {
            // Modify
            return Double.MAX_VALUE;
        }

        return Double.MAX_VALUE;
    }

    private double distanceToClosestElem(List<?> list, Object value) {
        double minDist = Double.MAX_VALUE;

        for (Object o : list) {
            double dif = compareValues(o, value);
            double absDif = abs(dif);
            if (absDif < minDist) minDist = absDif;
        }
        return minDist;
    }
}