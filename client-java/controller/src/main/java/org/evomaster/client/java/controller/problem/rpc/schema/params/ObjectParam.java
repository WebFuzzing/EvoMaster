package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
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
            for (NamedTypedValue v: getValue()){
                Field f = clazz.getDeclaredField(v.getName());
                f.setAccessible(true);
                Object vins = v.newInstance();
                if (vins != null)
                    f.set(instance, vins);
            }
            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException("fail to construct the class:"+clazzName+" with error msg:"+e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("fail to access the class:"+clazzName+" with error msg:"+e.getMessage());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("fail to access the field:"+clazzName+" with error msg:"+e.getMessage());
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
        if (getValue() != null)
            dto.innerContent = getValue().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        else
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
        List<NamedTypedValue> values = new ArrayList<>();
        List<NamedTypedValue> fields = getType().getFields();
        for (NamedTypedValue f: fields){
            NamedTypedValue copy = f.copyStructure();
            try {
                Field fi = instance.getClass().getDeclaredField(f.getName());
                fi.setAccessible(true);
                Object fiv = fi.get(instance);
                copy.setValueBasedOnValidInstance(fiv);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to get value of the field with the name ("+ f.getName()+ ") and error Msg:"+e.getMessage());
            }

            values.add(copy);
        }

        setValue(values);
    }
}
