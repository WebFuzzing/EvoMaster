package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

/**
 * map type
 */
public class MapType extends TypeSchema{
    /**
     * template of keys of the map
     */
    private final NamedTypedValue keyTemplate;
    /**
     * template of values of the map
     */
    private final NamedTypedValue valueTemplate;

    public MapType(String type, String fullTypeName, NamedTypedValue keyTemplate, NamedTypedValue valueTemplate) {
        super(type, fullTypeName);
        this.keyTemplate = keyTemplate;
        this.valueTemplate = valueTemplate;
    }

    public NamedTypedValue getKeyTemplate() {
        return keyTemplate;
    }

    public NamedTypedValue getValueTemplate() {
        return valueTemplate;
    }
}
