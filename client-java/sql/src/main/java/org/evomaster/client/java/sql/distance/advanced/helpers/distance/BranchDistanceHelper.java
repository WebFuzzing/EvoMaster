package org.evomaster.client.java.sql.distance.advanced.helpers.distance;

import static java.lang.Double.MAX_VALUE;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.evomaster.client.java.sql.distance.advanced.helpers.ConversionsHelper.convertToBoolean;
import static org.evomaster.client.java.sql.distance.advanced.helpers.ConversionsHelper.convertToDouble;

public class BranchDistanceHelper {

    public static Double calculateDistanceForEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return left == right ? 0D : MAX_VALUE;
        } else {
            return calculateDistanceForNonNullEquals(left, right);
        }
    }

    public static Double calculateDistanceForNonNullEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleDistanceHelper.calculateDistanceForEquals(convertToDouble(left), convertToDouble(right));
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return BooleanDistanceHelper.calculateDistanceForEquals(convertToBoolean(left), convertToBoolean(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Double calculateDistanceForNotEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return left != right ? 0D : MAX_VALUE;
        } else {
            return calculateDistanceForNonNullNotEquals(left, right);
        }
    }

    public static Double calculateDistanceForNonNullNotEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleDistanceHelper.calculateDistanceForNotEquals(convertToDouble(left), convertToDouble(right));
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return BooleanDistanceHelper.calculateDistanceForNotEquals(convertToBoolean(left), convertToBoolean(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported not equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Double calculateDistanceForGreaterThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return MAX_VALUE;
        } else {
            return calculateDistanceForNonNullGreaterThan(left, right);
        }
    }

    public static Double calculateDistanceForNonNullGreaterThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleDistanceHelper.calculateDistanceForGreaterThan(convertToDouble(left), convertToDouble(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Double calculateDistanceForGreaterThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return MAX_VALUE;
        } else {
            return calculateDistanceForNonNullGreaterThanOrEquals(left, right);
        }
    }

    public static Double calculateDistanceForNonNullGreaterThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleDistanceHelper.calculateDistanceForGreaterThanOrEquals(convertToDouble(left), convertToDouble(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Double calculateDistanceForMinorThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return MAX_VALUE;
        } else {
            return calculateDistanceForNonNullMinorThan(left, right);
        }
    }

    public static Double calculateDistanceForNonNullMinorThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleDistanceHelper.calculateDistanceForMinorThan(convertToDouble(left), convertToDouble(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Double calculateDistanceForMinorThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return MAX_VALUE;
        } else {
            return calculateDistanceForNonNullMinorThanOrEquals(left, right);
        }
    }

    public static Double calculateDistanceForNonNullMinorThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleDistanceHelper.calculateDistanceForMinorThanOrEquals(convertToDouble(left), convertToDouble(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than or equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Double aggregateDistancesForAnd(Double aConditionDistance, Double otherConditionDistance) {
        return aConditionDistance + otherConditionDistance;
    }

    public static Double aggregateDistancesForOr(Double aConditionDistance, Double otherConditionDistance) {
        return Math.min(aConditionDistance, otherConditionDistance);
    }

    public static Double normalize(Double distance) {
        return distance / (distance + 1);
    }
}