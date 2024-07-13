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

    public Truthness calculateEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateNull(left, right);
        } else {
            return trueOrScaleTrue(calculateNonNullEquals(left, right).getOfTrue());
        }
    }

    private Truthness calculateNonNullEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateEquals(convertToDouble(left), convertToDouble(right));
        } else if(left instanceof String && right instanceof String) {
            return stringComparisonCalculator.calculateEquals((String) left, (String) right);
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return booleanComparisonCalculator.calculateEquals(convertToBoolean(left), convertToBoolean(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateNotEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateNull(left, right);
        } else {
            return trueOrScaleTrue(calculateNonNullNotEquals(left, right).getOfTrue());
        }
    }

    private Truthness calculateNonNullNotEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateNotEquals(convertToDouble(left), convertToDouble(right));
        } else if(left instanceof String && right instanceof String) {
            return stringComparisonCalculator.calculateNotEquals((String) left, (String) right);
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return booleanComparisonCalculator.calculateNotEquals(convertToBoolean(left), convertToBoolean(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateNotEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported not equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateGreaterThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateNull(left, right);
        } else {
            return trueOrScaleTrue(calculateNonNullGreaterThan(left, right).getOfTrue());
        }
    }

    private Truthness calculateNonNullGreaterThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateGreaterThan(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateGreaterThan(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateGreaterThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateNull(left, right);
        } else {
            return trueOrScaleTrue(calculateNonNullGreaterThanOrEquals(left, right).getOfTrue());
        }
    }

    private Truthness calculateNonNullGreaterThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateGreaterThanOrEquals(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateGreaterThanOrEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateMinorThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateNull(left, right);
        } else {
            return trueOrScaleTrue(calculateNonNullMinorThan(left, right).getOfTrue());
        }
    }

    private Truthness calculateNonNullMinorThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateMinorThan(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateMinorThan(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public Truthness calculateMinorThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateNull(left, right);
        } else {
            return trueOrScaleTrue(calculateNonNullMinorThanOrEquals(left, right).getOfTrue());
        }
    }

    private Truthness calculateNonNullMinorThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return doubleComparisonCalculator.calculateMinorThanOrEquals(convertToDouble(left), convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return dateComparisonCalculator.calculateMinorThanOrEquals(convertToDate(left), convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than or equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    private Truthness calculateNull(Object left, Object right) {
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