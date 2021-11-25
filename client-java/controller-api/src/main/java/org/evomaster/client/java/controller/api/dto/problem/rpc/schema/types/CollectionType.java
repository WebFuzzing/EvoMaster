package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

/**
 * collection type which includes Array, List, Set
 */
public class CollectionType extends TypeSchema{
    /**
     * template of elements of the collection
     */
    private final NamedTypedValue template;

    public CollectionType(String type, String fullTypeName, NamedTypedValue template) {
        super(type, fullTypeName);
        this.template = template;
    }

    public NamedTypedValue getTemplate() {
        return template;
    }
}
