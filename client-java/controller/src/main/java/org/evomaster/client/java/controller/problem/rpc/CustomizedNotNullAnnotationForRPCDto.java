package org.evomaster.client.java.controller.problem.rpc;

/**
 * a customized criteria to check if a field is required or (not nullable) with the following
 * three situations
 * - only [annotationType] is specified, then the field with such annotation
 *      would be considered as `required`
 * - [annotationType], [annotationMethod] and [equalsTo] are specified, then
 *      the field is considered as `required` only if the field is applied with the annotation
 *      and the method of the annotation returns the same value as [equalsTo]
 */
public class CustomizedNotNullAnnotationForRPCDto {

    /**
     * name of the annotation
     * it is not nullable
     */
    public String annotationType;

    /**
     * name of method of the annotation
     * it is nullable
     */
    public String annotationMethod;

    /**
     * value of the specified field or method
     * note that it is nullable,
     * but if annotationMethod] is not null,
     * this must not be null
     */
    public Object equalsTo;
}
