package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * Primitive types Param
 */
public abstract class PrimitiveOrWrapperParam<V> extends NamedTypedValue<PrimitiveOrWrapperType, V> {

    public PrimitiveOrWrapperParam(String name, String type, String fullTypeName, Class<?> clazz){
        super(name, new PrimitiveOrWrapperType(type, fullTypeName, clazz));
        // primitive type is not nullable
        setNullable(!getType().isWrapper);

    }

    public PrimitiveOrWrapperParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
        // primitive type is not nullable
        setNullable(!getType().isWrapper);
    }

    public static PrimitiveOrWrapperParam build(String name, Class<?> clazz){
        if (clazz == Integer.class || clazz == int.class)
            return new IntParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Boolean.class || clazz == boolean.class)
            return new BooleanParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Double.class || clazz == double.class)
            return new DoubleParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Float.class || clazz == float.class)
            return new FloatParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Long.class || clazz == long.class)
            return new LongParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Character.class || clazz == char.class)
            return new CharParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Byte.class || clazz == byte.class)
            return new ByteParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        if (clazz == Short.class || clazz == short.class)
            return new ShortParam(name, clazz.getSimpleName(), clazz.getName(), clazz);
        throw new RuntimeException("PrimitiveOrWrapperParam: unhandled type "+ clazz.getName());
    }

    @Override
    public Object newInstance() {
        return getValue();
    }
}
