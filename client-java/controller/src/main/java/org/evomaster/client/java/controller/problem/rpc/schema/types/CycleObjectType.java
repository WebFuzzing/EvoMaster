package org.evomaster.client.java.controller.problem.rpc.schema.types;


import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

import java.util.ArrayList;
import java.util.List;

/**
 * cycle object
 */
public class CycleObjectType extends ObjectType{

    public CycleObjectType(String type, String fullTypeName, Class<?> clazz, List<String> genericTypes) {
        super(type, fullTypeName, new ArrayList<>(), clazz, genericTypes);
    }

    public CycleObjectType copyContent(){
        List<String> genericTypes = this.genericTypes != null? new ArrayList<>(this.genericTypes): null;
        return new CycleObjectType(getType(), getFullTypeName(), getClazz(), genericTypes);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.CUSTOM_CYCLE_OBJECT;
        return dto;
    }

}
