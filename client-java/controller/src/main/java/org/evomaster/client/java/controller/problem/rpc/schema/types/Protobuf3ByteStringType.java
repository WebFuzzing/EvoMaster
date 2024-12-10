package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;


public class Protobuf3ByteStringType extends TypeSchema {
    public final static String PROTOBUF3_BYTE_STRING_SIMPLE_TYPE_NAME = "ByteString";
    public final static String PROTOBUF3_BYTE_STRING_TYPE_NAME = "com.google.protobuf.ByteString";

    private static Protobuf3ByteStringType instance;
    public static Protobuf3ByteStringType getInstance(JavaDtoSpec spec, Class<?> clazz){
        if (instance == null)
            instance = new Protobuf3ByteStringType(spec, clazz);
        return instance;
    }

    public Protobuf3ByteStringType(JavaDtoSpec spec, Class<?> clazz) {
        super(PROTOBUF3_BYTE_STRING_SIMPLE_TYPE_NAME, PROTOBUF3_BYTE_STRING_TYPE_NAME, clazz, spec);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.BYTEBUFFER;
        return dto;
    }

    @Override
    public Protobuf3ByteStringType copy() {
        return new Protobuf3ByteStringType(spec, getClazz());
    }
}
