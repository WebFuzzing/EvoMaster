package org.evomaster.client.java.sql.advanced.helpers.truthness;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper;

import java.time.LocalDateTime;
import java.util.Date;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.evomaster.client.java.distance.heuristics.Truthness.*;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;

public class ObjectTruthnessHelper {

    public static Truthness calculateTruthnessForEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullEquals(left, right).getOfTrue());
        }
    }

    public static Truthness calculateTruthnessForNonNullEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleTruthnessHelper.calculateTruthnessForEquals(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
        } else if(left instanceof String && right instanceof String) {
            return StringTruthnessHelper.calculateTruthnessForEquals((String) left, (String) right);
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return BooleanTruthnessHelper.calculateTruthnessForEquals(ConversionsHelper.convertToBoolean(left), ConversionsHelper.convertToBoolean(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return DateTruthnessHelper.calculateTruthnessForEquals(ConversionsHelper.convertToDate(left), ConversionsHelper.convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Truthness calculateTruthnessForNotEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullNotEquals(left, right).getOfTrue());
        }
    }

    public static Truthness calculateTruthnessForNonNullNotEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleTruthnessHelper.calculateTruthnessForNotEquals(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
        } else if(left instanceof String && right instanceof String) {
            return StringTruthnessHelper.calculateTruthnessForNotEquals((String) left, (String) right);
        } else if(left instanceof Boolean || right instanceof Boolean) {
            return BooleanTruthnessHelper.calculateTruthnessForNotEquals(ConversionsHelper.convertToBoolean(left), ConversionsHelper.convertToBoolean(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return DateTruthnessHelper.calculateTruthnessForNotEquals(ConversionsHelper.convertToDate(left), ConversionsHelper.convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported not equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Truthness calculateTruthnessForGreaterThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullGreaterThan(left, right).getOfTrue());
        }
    }

    public static Truthness calculateTruthnessForNonNullGreaterThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleTruthnessHelper.calculateTruthnessForGreaterThan(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return DateTruthnessHelper.calculateTruthnessForGreaterThan(ConversionsHelper.convertToDate(left), ConversionsHelper.convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Truthness calculateTruthnessForGreaterThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullGreaterThanOrEquals(left, right).getOfTrue());
        }
    }

    public static Truthness calculateTruthnessForNonNullGreaterThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleTruthnessHelper.calculateTruthnessForGreaterThanOrEquals(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return DateTruthnessHelper.calculateTruthnessForGreaterThanOrEquals(ConversionsHelper.convertToDate(left), ConversionsHelper.convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported greater than equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Truthness calculateTruthnessForMinorThan(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullMinorThan(left, right).getOfTrue());
        }
    }

    public static Truthness calculateTruthnessForNonNullMinorThan(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleTruthnessHelper.calculateTruthnessForMinorThan(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return DateTruthnessHelper.calculateTruthnessForMinorThan(ConversionsHelper.convertToDate(left), ConversionsHelper.convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    public static Truthness calculateTruthnessForMinorThanOrEquals(Object left, Object right) {
        if(isNull(left) || isNull(right)) {
            return calculateTruthnessForNull(left, right);
        } else {
            return trueOrScaleTrue(calculateTruthnessForNonNullMinorThanOrEquals(left, right).getOfTrue());
        }
    }

    public static Truthness calculateTruthnessForNonNullMinorThanOrEquals(Object left, Object right) {
        if(left instanceof Number && right instanceof Number) {
            return DoubleTruthnessHelper.calculateTruthnessForMinorThanOrEquals(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
        } else if(isInstanceOfDate(left) || isInstanceOfDate(right)) {
            return DateTruthnessHelper.calculateTruthnessForMinorThanOrEquals(ConversionsHelper.convertToDate(left), ConversionsHelper.convertToDate(right));
        } else {
            throw new UnsupportedOperationException(
                format("Unsupported minor than or equals calculation between values %s (%s) and %s (%s)",
                    left, left.getClass().getName(), right, right.getClass().getName()));
        }
    }

    private static Truthness calculateTruthnessForNull(Object left, Object right) {
        if(isNull(left) && isNull(right)) {
            return FALSE_LOWER;
        } else {
            return FALSE;
        }
    }

    private static Boolean isInstanceOfDate(Object object) {
        return object instanceof Date || object instanceof LocalDateTime;
    }
}