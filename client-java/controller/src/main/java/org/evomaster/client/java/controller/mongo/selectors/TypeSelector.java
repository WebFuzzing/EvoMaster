package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.bsonTypeValueOf;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getTypeFromNumber;

/**
 * { field: { $type: BSON type } }
 */
public class TypeSelector extends SingleConditionQuerySelector {

    public static final String TYPE_OPERATOR = "$type";

    /**
     * BSON type aliases for numeric types. The alias "number" is used to represent any numeric BSON type,
     * including double, int, long, and decimal.
     */
    private static final String NUMBER = "number";
    private static final String DOUBLE = "double";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String DECIMAL = "decimal";
    private static final String STRING = "string";
    private static final String OBJECT = "object";
    private static final String ARRAY = "array";
    private static final String BIN_DATA = "binData";
    private static final String UNDEFINED = "undefined";
    private static final String OBJECT_ID = "objectId";
    private static final String BOOL = "bool";
    private static final String DATE = "date";
    private static final String NULL = "null";
    private static final String REGEX = "regex";
    private static final String DB_POINTER = "dbPointer";
    private static final String JAVASCRIPT = "javascript";
    private static final String SYMBOL = "symbol";
    private static final String JAVASCRIPT_WITH_SCOPE = "javascriptWithScope";
    private static final String TIMESTAMP = "timestamp";
    private static final String MIN_KEY = "minKey";
    private static final String MAX_KEY = "maxKey";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        List<Object> parsedBsonTypes = new LinkedList<>();
        if (value instanceof List<?>) {
            List<?> valueList = (List<?>) value;
            for (Object valueListElement : valueList) {
                List<Object> bsonTypes = parseToBsonTypes(valueListElement);
                if (bsonTypes == null) {
                    return null;
                }
                parsedBsonTypes.addAll(bsonTypes);
            }
        } else {
            List<Object> bsonTypes = parseToBsonTypes(value);
            if (bsonTypes == null) {
                return null;
            }
            parsedBsonTypes.addAll(bsonTypes);
        }
        if (parsedBsonTypes.isEmpty()) {
            return null;
        }
        return new TypeOperation(fieldName, parsedBsonTypes);
    }


    /**
     * Parses the given value to determine the corresponding BSON type(s).
     * As the "number" alias is used to represent any numeric BSON type,
     * a list of BsonTypes is returned instead of a single BsonType.
     *
     * @param value
     * @return
     */
    public static List<Object> parseToBsonTypes(Object value) {
        if (value instanceof String) {
            String alias = (String) value;
            if (alias.equals(NUMBER)) {
                /**
                 * The "number" alias is used to represent any numeric BSON type, including double, int, long, and decimal.
                 * Therefore, when the alias is "number", a list of all numeric BSON types is returned instead of a single BSON type.
                 * This allows for more flexible queries that can match any numeric type in the database, rather than being limited to a specific numeric type.
                 * The list of BSON types returned for the "number" alias includes DOUBLE, INT, LONG, and DECIMAL.
                 */
                List<Object> bsonTypes = new LinkedList<>();
                bsonTypes.addAll(parseToBsonTypes(DOUBLE));
                bsonTypes.addAll(parseToBsonTypes(INT));
                bsonTypes.addAll(parseToBsonTypes(LONG));
                bsonTypes.addAll(parseToBsonTypes(DECIMAL));
                return bsonTypes;
            } else {
                String enumName = getBsonTypeEnumNameFromAlias(alias);
                if (enumName == null) {
                    return null;
                }
                final Object bsonType = bsonTypeValueOf(enumName);
                if (bsonType == null) {
                    return null;
                }
                return Arrays.asList(bsonType);
            }
        } else if (value instanceof Number) {
            final Object bsonType = getTypeFromNumber(((Number) value).intValue());
            if (bsonType == null) {
                return null;
            }
            return Arrays.asList(bsonType);
        } else {
            return null;
        }
    }

    static String getBsonTypeEnumNameFromAlias(String alias) {

        switch (alias) {
            case DOUBLE:
                return "DOUBLE";
            case STRING:
                return "STRING";
            case OBJECT:
                return "DOCUMENT";
            case ARRAY:
                return "ARRAY";
            case BIN_DATA:
                return "BINARY";
            case UNDEFINED:
                return "UNDEFINED";
            case OBJECT_ID:
                return "OBJECT_ID";
            case BOOL:
                return "BOOLEAN";
            case DATE:
                return "DATE_TIME";
            case NULL:
                return "NULL";
            case REGEX:
                return "REGULAR_EXPRESSION";
            case DB_POINTER:
                return "DB_POINTER";
            case JAVASCRIPT:
                return "JAVASCRIPT";
            case SYMBOL:
                return "SYMBOL";
            case JAVASCRIPT_WITH_SCOPE:
                return "JAVASCRIPT_WITH_SCOPE";
            case INT:
                return "INT32";
            case TIMESTAMP:
                return "TIMESTAMP";
            case LONG:
                return "INT64";
            case DECIMAL:
                return "DECIMAL128";
            case MIN_KEY:
                return "MIN_KEY";
            case MAX_KEY:
                return "MAX_KEY";
            default:
                // If the alias is not recognized, return null to indicate an invalid BSON alias name.
                return null;
        }
    }

    @Override
    protected String operator() {
        return TYPE_OPERATOR;
    }
}
