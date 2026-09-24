package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.controller.problem.rpc.schema.params.BigDecimalParam;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * { field: { $exists: boolean } }
 */
public class ExistsSelector extends SingleConditionQuerySelector {

    public static final String EXISTS_OPERATOR = "$exists";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);

        boolean truthy = isTruthy(value);
        return new ExistsOperation(fieldName, truthy);
    }

    @Override
    protected String operator() {
        return EXISTS_OPERATOR;
    }

    static boolean isTruthy(Object v) {
        if (v == null) {
            return false;   // null / missing field
        }

        if (BsonHelper.isBsonUndefined(v)) {
            return false;
        }

        if (v instanceof Boolean) {
            return (Boolean) v;
        }

        if (v instanceof Number && BsonHelper.isDecimal128(v)) {
            // NaN and Infinity are non-zero, so truthy
            if (BsonHelper.isNaN(v) || BsonHelper.isInfinite(v)) {
                return true;
            }
            BigDecimal d = BsonHelper.getBigDecimalValue((Number) v);
            return d.signum() != 0;
        }

        if (v instanceof BigDecimal) {
            BigDecimal d = (BigDecimal) v;
            return d.signum() != 0;
        }

        if (v instanceof BigInteger) {
            BigInteger i = (BigInteger) v;
            return i.signum() != 0;
        }

        if (v instanceof Number) {
            Number n = (Number) v;
            return n.doubleValue() != 0.0;  // NaN != 0.0 → true
        }

        return true;    // strings (incl. ""), lists (incl. []), maps, dates, ObjectId, ...
    }
}
