package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.CollectionType;

import java.nio.ByteBuffer;

/**
 * this is created for handling binary in thrift, see https://thrift.apache.org/docs/types
 */
public class ByteBufferParam extends NamedTypedValue<CollectionType, ByteBuffer>{
    private static final ByteParam template = new ByteParam("template");

    public ByteBufferParam(String name) {
        super(name, new CollectionType(ByteBuffer.class.getSimpleName(), ByteBuffer.class.getName(), template));
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
        dto.type.example = getType().getTemplate().getDto();
        return dto;
    }
}
