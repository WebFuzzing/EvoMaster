package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.ObjectType;
import java.lang.reflect.Field;

/**
 * object param
 */
public class ObjectParam extends NamedTypedValue<ObjectType, Object> {

    public ObjectParam(String name, ObjectType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        String clazzName = getType().getFullTypeName();
        Class<?> clazz = Class.forName(clazzName);
        try {
            Object instance = clazz.newInstance();
            Field[] fs = clazz.getDeclaredFields();
            for (int i = 0; i<fs.length; i++ ){
                Field f = fs[i];
                NamedTypedValue v = getType().getFields().get(i);
                f.setAccessible(true);
                f.set(instance, v.newInstance());
            }
            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException("fail to construct the class:"+clazzName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("fail to access the class:"+clazzName);
        }
    }
}
