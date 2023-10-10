package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

/**
 * string type
 */
public class StringType extends TypeSchema {
    private final static String STRING_TYPE_NAME = String.class.getSimpleName();
    private final static String FULL_STRING_TYPE_NAME = String.class.getName();


    public StringType(JavaDtoSpec spec) {
        super(STRING_TYPE_NAME, FULL_STRING_TYPE_NAME, String.class, spec);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.STRING;
        return dto;
    }

    @Override
    public StringType copy() {
        return new StringType(spec);
    }

    @Override
    public String getTypeNameForInstanceInJavaOrKotlin(boolean isJava) {
        return getSimpleTypeName();
    }
}
