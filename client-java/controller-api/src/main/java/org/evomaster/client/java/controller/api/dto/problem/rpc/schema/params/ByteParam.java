package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

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
}
