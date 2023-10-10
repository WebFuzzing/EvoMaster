package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

import java.nio.ByteBuffer;

/**
 * a type schema for java.nio.ByteBuffer
 */
public class ByteBufferType extends TypeSchema {
    public final static String BYTEBUFFER_TYPE_NAME = ByteBuffer.class.getSimpleName();
    public final static String BYTEBUFFER_STRING_TYPE_NAME = ByteBuffer.class.getName();


    public ByteBufferType(JavaDtoSpec spec) {
        super(BYTEBUFFER_TYPE_NAME, BYTEBUFFER_STRING_TYPE_NAME, ByteBuffer.class, spec);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.BYTEBUFFER;
        return dto;
    }

    @Override
    public ByteBufferType copy() {
        return new ByteBufferType(spec);
    }
}
