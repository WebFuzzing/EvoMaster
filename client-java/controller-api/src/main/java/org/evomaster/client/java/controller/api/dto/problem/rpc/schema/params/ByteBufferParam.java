package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.StringType;

import java.nio.ByteBuffer;

/**
 * this is created for handling binary in thrift, see https://thrift.apache.org/docs/types
 * handle it as string
 */
public class ByteBufferParam extends NamedTypedValue<StringType, ByteBuffer>{

    public ByteBufferParam(String name) {
        super(name, new StringType());
    }

    public void setValue(byte[] value) {
        ByteBuffer buffer = ByteBuffer.allocate(value.length);
        buffer.put(value);
        this.setValue(buffer);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return getValue();
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.BYTEBUFFER;
        return dto;
    }

    @Override
    public ByteBufferParam copyStructure() {
        return new ByteBufferParam(getName());
    }

    @Override
    public void setValue(ParamDto dto) {
        setValue(dto.jsonValue.getBytes());
    }
}
