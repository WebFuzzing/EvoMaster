package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.PairParam;

import java.util.Arrays;
import java.util.Map;

/**
 * map type
 */
public class MapType extends TypeSchema{
    /**
     * template of keys of the map
     */
    private final PairParam template;


    public MapType(String type, String fullTypeName, PairParam template, Class<?> clazz) {
        super(type, fullTypeName, clazz);
        this.template = template;
    }

    public PairParam getTemplate() {
        return template;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        ParamDto example = template.getDto();
        example.innerContent = Arrays.asList(template.getType().getFirstTemplate().getDto(), template.getType().getSecondTemplate().getDto());
        dto.example = example;
        return dto;
    }

    @Override
    public String getTypeNameForInstance() {
        String key = template.getType().getFirstTemplate().getType().getTypeNameForInstance();
        String value = template.getType().getSecondTemplate().getType().getTypeNameForInstance();
        return Map.class.getName()+"<"+key+","+value+">";
    }

    @Override
    public MapType copy() {
        return new MapType(getType(),getFullTypeName(), template, getClazz());
    }
}
