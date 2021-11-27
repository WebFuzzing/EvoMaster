package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

/**
 * map type
 */
public class MapType extends TypeSchema{
    /**
     * template of keys of the map
     */
    private final NamedTypedValue template;


    public MapType(String type, String fullTypeName, NamedTypedValue template) {
        super(type, fullTypeName);
        this.template = template;
    }

    public NamedTypedValue getTemplate() {
        return template;
    }
}
