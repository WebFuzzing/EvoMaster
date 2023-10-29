package org.evomaster.client.java.instrumentation.object;

/**
 * Defines how a non-default Java type should be converted into an OpenAPI Specification (OAS) schema.
 */
public abstract class CustomTypeToOasConverter {

    /**
     * Converts the custom type into its representation in the OpenAPI Specification schema.
     *
     * @return A string representing the type in the OpenAPI Specification schema.
     */
    public abstract String convert();

    /**
     * Checks whether the provided class matches the type for which the converter is designed to
     * create an OpenAPI Specification schema.
     *
     * @param klass The class to be checked for compatibility.
     * @return true if the class is compatible, false otherwise.
     */
    public abstract boolean isInstanceOf(Class<?> klass);
}
