package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * object param
 */
public class ObjectParam extends NamedTypedValue<ObjectType, List<NamedTypedValue>> {

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
                NamedTypedValue v = getValue().get(i);
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

    @Override
    public ObjectParam copyStructure() {
        return new ObjectParam(getName(), getType());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.CUSTOM_OBJECT;
        dto.innerContent = getType().getFields().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        return dto;
    }

    @Override
    public void setValue(ParamDto dto) {

        if (dto.innerContent!=null && !dto.innerContent.isEmpty()){
            List<NamedTypedValue> fields = getType().getFields();
            List<NamedTypedValue> values = new ArrayList<>();

            for (ParamDto p: dto.innerContent){
                NamedTypedValue f = fields.stream().filter(s-> s.sameParam(p)).findFirst().get().copyStructure();
                f.setValue(p);
                values.add(f);
            }

            setValue(values);
        }


    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        // TODO
    }
}
