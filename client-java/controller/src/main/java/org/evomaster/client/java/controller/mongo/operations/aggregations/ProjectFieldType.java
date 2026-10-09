package org.evomaster.client.java.controller.mongo.operations.aggregations;

/**
 * The way a field is used in a $project stage.
 */
public enum ProjectFieldType {
    /** The field is kept (1 or true) */
    INCLUDE,
    /** The field is removed (0 or false) */
    EXCLUDE,
    /** The field is the result of an expression */
    COMPUTED
}
