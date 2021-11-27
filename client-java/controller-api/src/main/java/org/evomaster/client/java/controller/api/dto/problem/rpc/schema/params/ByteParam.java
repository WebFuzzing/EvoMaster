package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * byte param
 */
public class ByteParam extends PrimitiveOrWrapperParam<Byte> {

    public final static String TYPE_NAME = byte.class.getSimpleName();
    public final static String FULL_TYPE_NAME = byte.class.getName();

    public ByteParam(String name) {
        this(name, TYPE_NAME, FULL_TYPE_NAME);
    }

    public ByteParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public ByteParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.BYTE;
        else
            dto.type.type = RPCSupportedDataType.P_BYTE;
        return dto;
    }

    @Override
    public ByteParam copyStructure() {
        return new ByteParam(getName(), getType());
    }
}
