package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.time.LocalDateTime;
import java.util.Date;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE_BETTER;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.trueOrScaleTrue;
import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.*;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators.BooleanComparisonCalculator.createBooleanComparisonCalculator;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators.DateComparisonCalculator.createDateComparisonCalculator;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators.DoubleComparisonCalculator.createDoubleComparisonCalculator;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators.StringComparisonCalculator.createStringComparisonCalculator;

public class ObjectComparisonCalculator {

    private BooleanComparisonCalculator booleanComparisonCalculator;
    private DateComparisonCalculator dateComparisonCalculator;
    private DoubleComparisonCalculator doubleComparisonCalculator;
    private StringComparisonCalculator stringComparisonCalculator;

    public ObjectComparisonCalculator(
            BooleanComparisonCalculator booleanComparisonCalculator,
            DateComparisonCalculator dateComparisonCalculator,
            DoubleComparisonCalculator doubleComparisonCalculator,
            StringComparisonCalculator stringComparisonCalculator) {
        this.booleanComparisonCalculator = booleanComparisonCalculator;
        this.dateComparisonCalculator = dateComparisonCalculator;
        this.doubleComparisonCalculator = doubleComparisonCalculator;
        this.stringComparisonCalculator = stringComparisonCalculator;
    }

    public static ObjectComparisonCalculator createObjectTruthnessCalculator(TaintHandler taintHandler){
        return new ObjectComparisonCalculator(
            createBooleanComparisonCalculator(),
            createDateComparisonCalculator(),
            createDoubleComparisonCalculator(),
            createStringComparisonCalculator(taintHandler));
    }

    public Truthness calculateTruthnessForEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullEquals(left, right).getOfTrue());
        }
    }

    public Truthness calculateTruthnessForNonNullEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateTruthnessForEquals(convertToDouble(left), convertToDouble(right));
        } else if(left instanceof String && right instanceof String) {
            return stringComparisonCalculator.calculateTruthnessForEquals((String) left, (String) right);
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return booleanComparisonCalculator.calculateTruthnessForEquals(convertToBoolean(left), convertToBoolean(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateTruthnessForEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateTruthnessForNotEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullNotEquals(left, right).getOfTrue());
        }
    }

    public Truthness calculateTruthnessForNonNullNotEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateTruthnessForNotEquals(convertToDouble(left), convertToDouble(right));
        } else if(left instanceof String && right instanceof String) {
            return stringComparisonCalculator.calculateTruthnessForNotEquals((String) left, (String) right);
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return booleanComparisonCalculator.calculateTruthnessForNotEquals(convertToBoolean(left), convertToBoolean(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateTruthnessForNotEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported not equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateTruthnessForGreaterThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullGreaterThan(left, right).getOfTrue());
        }
    }

    public Truthness calculateTruthnessForNonNullGreaterThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateTruthnessForGreaterThan(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateTruthnessForGreaterThan(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateTruthnessForGreaterThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullGreaterThanOrEquals(left, right).getOfTrue());
        }
    }

    public Truthness calculateTruthnessForNonNullGreaterThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateTruthnessForGreaterThanOrEquals(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateTruthnessForGreaterThanOrEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateTruthnessForMinorThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullMinorThan(left, right).getOfTrue());
        }
    }

    public Truthness calculateTruthnessForNonNullMinorThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateTruthnessForMinorThan(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateTruthnessForMinorThan(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateTruthnessForMinorThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullMinorThanOrEquals(left, right).getOfTrue());
        }
    }

    public Truthness calculateTruthnessForNonNullMinorThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateTruthnessForMinorThanOrEquals(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateTruthnessForMinorThanOrEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than or equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    private Truthness calculateTruthnessForNull(Object left, Object right) {
        if(isNull(left) && isNull(right)) {
            return FALSE;
        } else {
            return FALSE_BETTER;
        }
    }

    private Boolean isInstanceOfDate(Object object) {
        return object instanceof Date || object instanceof LocalDateTime;
    }
}