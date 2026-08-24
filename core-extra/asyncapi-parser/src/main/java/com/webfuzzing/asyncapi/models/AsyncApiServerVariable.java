package com.webfuzzing.asyncapi.models;

import java.util.Collections;
import java.util.List;

/**
 * One entry under {@code servers.<server>.variables}. Same shape as an OpenAPI server variable.
 */
public class AsyncApiServerVariable {

    private final String name;

    private final String defaultValue;

    /**
     * The closed set of values this variable may take, when one is declared, in declaration
     * order. Empty when the document declares none. Named for the {@code enum} field, which is
     * a Java keyword.
     */
    private final List<String> enumeration;

    private final String description;

    public AsyncApiServerVariable(
            String name,
            String defaultValue,
            List<String> enumeration,
            String description) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.enumeration = Collections.unmodifiableList(enumeration);
        this.description = description;
    }

    public String getName() {
        return name;
    }

    /**
     * The document's {@code default} field, under a name that is not a Java keyword.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * The closed set of values the variable may take, when one is declared. Named for the
     * document's {@code enum} field, which is a Java keyword.
     */
    public List<String> getEnumeration() {
        return enumeration;
    }

    public String getDescription() {
        return description;
    }
}
