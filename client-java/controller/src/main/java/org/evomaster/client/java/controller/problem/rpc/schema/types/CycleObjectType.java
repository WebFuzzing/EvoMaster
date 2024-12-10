package org.evomaster.client.java.controller.problem.rpc.schema.types;


import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

import java.util.ArrayList;
import java.util.List;

/**
 * cycle object
 */
public class CycleObjectType extends ObjectType{

    public CycleObjectType(String type, String fullTypeName, Class<?> clazz, List<String> genericTypes, JavaDtoSpec spec) {
        super(type, fullTypeName, new ArrayList<>(), clazz, genericTypes, spec);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.CUSTOM_CYCLE_OBJECT;
        return dto;
    }

}
