package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ByteBufferType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * this is created for handling binary in thrift, see https://thrift.apache.org/docs/types
 * handle it as string
 */
public class ByteBufferParam extends NamedTypedValue<ByteBufferType, ByteBuffer>{

    public ByteBufferParam(String name) {
        super(name, new ByteBufferType());
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
        if (getValue() != null){
            // bytebuffer is now handled as string
            dto.jsonValue = new String(getValue().array(), StandardCharsets.UTF_8);
        }

        return dto;
    }

    @Override
    public ByteBufferParam copyStructure() {
        return new ByteBufferParam(getName());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.jsonValue != null)
            setValue(dto.jsonValue.getBytes());
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((ByteBuffer) instance);
    }
}
