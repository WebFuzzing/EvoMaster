package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.PairParam;

/**
 * map type
 */
public class MapType extends TypeSchema{
    /**
     * template of keys of the map
     */
    private final PairParam template;


    public MapType(String type, String fullTypeName, PairParam template) {
        super(type, fullTypeName);
        this.template = template;
    }

    public PairParam getTemplate() {
        return template;
    }
}
