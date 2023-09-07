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

    private final static String KOTLIN_MAP = "MutableMap";

    /**
     * template of keys of the map
     */
    private final PairParam template;


    public MapType(String type, String fullTypeName, PairParam template, Class<?> clazz, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
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
    public String getTypeNameForInstanceInJavaOrKotlin(boolean isJava) {
        String key = template.getType().getFirstTemplate().getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        String value = template.getType().getSecondTemplate().getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        String type = getFullTypeName();
        if (!isJava)
            type = KOTLIN_MAP;

        return type+"<"+key+","+value+">";
    }

    @Override
    public MapType copy() {
        return new MapType(getSimpleTypeName(),getFullTypeName(), template, getClazz(), spec);
    }
}
