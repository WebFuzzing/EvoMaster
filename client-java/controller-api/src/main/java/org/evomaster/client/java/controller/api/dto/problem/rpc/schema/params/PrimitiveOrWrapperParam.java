package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * Primitive types Param
 */
public class PrimitiveOrWrapperParam<V> extends NamedTypedValue<PrimitiveOrWrapperType, V> {

    public PrimitiveOrWrapperParam(String name, String type, String fullTypeName){
        super(name, new PrimitiveOrWrapperType(type, fullTypeName));
    }

    public PrimitiveOrWrapperParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    public static PrimitiveOrWrapperParam build(String name, Class<?> clazz){
        if (clazz == Integer.class || clazz == int.class)
            return new IntParam(name, clazz.getSimpleName(), clazz.getName());
        if (clazz == Boolean.class || clazz == boolean.class)
            return new BooleanParam(name, clazz.getSimpleName(), clazz.getName());
        if (clazz == Double.class || clazz == double.class)
            return new DoubleParam(name, clazz.getSimpleName(), clazz.getName());
        if (clazz == Float.class || clazz == float.class)
            return new FloatParam(name, clazz.getSimpleName(), clazz.getName());
        if (clazz == Long.class || clazz == long.class)
            return new LongParam(name, clazz.getSimpleName(), clazz.getName());
        if (clazz == Character.class || clazz == char.class)
            return new CharParam(name, clazz.getSimpleName(), clazz.getName());
        if (clazz == Byte.class || clazz == byte.class)
            return new ByteParam(name, clazz.getSimpleName(), clazz.getName());
        throw new RuntimeException("PrimitiveOrWrapperParam: unhandled type "+ clazz.getName());
    }

    @Override
    public Object newInstance() {
        return getValue();
    }
}
