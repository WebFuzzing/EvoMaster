package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.TypeSchema;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * object param
 */
public class ObjectParam extends NamedTypedValue<ObjectType, List<NamedTypedValue>> {

    public ObjectParam(String name, ObjectType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        String clazzName = getType().getFullTypeName();
        Class<?> clazz = Class.forName(clazzName);
        try {
            Object instance = clazz.newInstance();
            for (NamedTypedValue v: getValue()){
                boolean setWithSetter = false;

                /*
                    if setter exists, we should prioritize the usage of the setter
                    for thrift, the setter contains additional info
                 */
                if(v.accessibleSchema != null && v.accessibleSchema.setterMethodName != null){
                    Method m =  getSetter(clazz, v.accessibleSchema.setterMethodName, v.getType(), v.getType().getClazz(), 0);
                            //clazz.getMethod(v.accessibleSchema.setterMethodName, v.getType().getClazz());
                    try {
                        m.invoke(instance, v.newInstance());
                        setWithSetter = true;
                    } catch (InvocationTargetException e) {
                        SimpleLogger.uniqueWarn("fail to access the method:"+clazzName+" with error msg:"+e.getMessage());
                    }
                }

                if (!setWithSetter){
                    Field f = clazz.getField(v.getName());
                    f.setAccessible(true);
                    Object vins = v.newInstance();
                    if (vins != null)
                        f.set(instance, vins);
                }

            }
            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException("fail to construct the class:"+clazzName+" with error msg:"+e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("fail to access the class:"+clazzName+" with error msg:"+e.getMessage());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("fail to access the field:"+clazzName+" with error msg:"+e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("fail to access the method:"+clazzName+" with error msg:"+e.getMessage());
        }
    }

    private Method getSetter(Class<?> clazz, String setterName, TypeSchema type, Class<?> typeClass, int attemptTimes) throws NoSuchMethodException {

        try {
            Method m = clazz.getMethod(setterName, type.getClazz());
            return m;
        } catch (NoSuchMethodException e) {
            if (type instanceof PrimitiveOrWrapperType && attemptTimes == 0){
                Type p = PrimitiveOrWrapperParam.getPrimitiveOrWrapper(type.getClazz());
                if (p instanceof Class){
                    return getSetter(clazz, setterName, type, (Class)p, 1);
                }
            }
            throw e;
        }
    }



    @Override
    public ObjectParam copyStructure() {
        return new ObjectParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();

        if (getValue() != null){
            dto.innerContent = getValue().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
            dto.setNotNullValue();
        } else
            dto.innerContent = getType().getFields().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        return dto;
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {

        if (dto.stringValue == null){
            setValue(null);
            return;
        }

        if (dto.innerContent!=null && !dto.innerContent.isEmpty()){
            List<NamedTypedValue> fields = getType().getFields();
            List<NamedTypedValue> values = new ArrayList<>();

            for (ParamDto p: dto.innerContent){
                NamedTypedValue f = fields.stream().filter(s-> s.sameParam(p)).findFirst().get().copyStructureWithProperties();
                f.setValueBasedOnDto(p);
                values.add(f);
            }

            setValue(values);
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        List<NamedTypedValue> values = new ArrayList<>();
        List<NamedTypedValue> fields = getType().getFields();
        Class<?> clazz;
        try {
            clazz = Class.forName(getType().getFullTypeName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ERROR: fail to get class with the name"+getType().getFullTypeName()+" Msg:"+e.getMessage());
        }
        for (NamedTypedValue f: fields){
            NamedTypedValue copy = f.copyStructureWithProperties();
            try {
                if (f.accessibleSchema == null || f.accessibleSchema.isAccessible){
                    Field fi = clazz.getField(f.getName());
                    fi.setAccessible(true);
                    Object fiv = fi.get(instance);
                    copy.setValueBasedOnInstance(fiv);
                } else if(f.accessibleSchema.getterMethodName != null){
                    Method m = clazz.getMethod(f.accessibleSchema.getterMethodName);
                    copy.setValueBasedOnInstance(m.invoke(instance));
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to get value of the field with the name ("+ f.getName()+ ") and error Msg:"+e.getMessage());
            } catch (NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("ERROR: fail to get/invoke getter method for the field with the name ("+ f.getName()+ ") and error Msg:"+e.getMessage());
            }

            values.add(copy);
        }

        setValue(values);
    }

    @Override
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException {

        Object instance = json;
        if (json instanceof String)
            instance = parseValueWithJson((String) json);

        if (instance == null){
            setValue(null); return;
        }

        if (isValidInstance(instance)){
            setValueBasedOnValidInstance(instance);
            return;
        }

        List<NamedTypedValue> values = new ArrayList<>();
        List<NamedTypedValue> fields = getType().getFields();

        /*
            in jackson, object would be extracted as a map
         */
        if (!(instance instanceof Map))
            throw new RuntimeException("cannot parse the map param "+getName()+ " with the type" + instance.getClass().getName());

        for (NamedTypedValue f: fields){
            NamedTypedValue copy = f.copyStructureWithProperties();
            Object fiv = ((Map)instance).get(f.getName());
            copy.setValueBasedOnInstanceOrJson(fiv);

            values.add(copy);
        }

        setValue(values);

    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String typeName = getType().getTypeNameForInstance();
        String varName = variableName;

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);
        String var = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, typeName, varName, null);
        CodeJavaGenerator.addCode(codes, var, indent);
        if (isNull) return codes;

        CodeJavaGenerator.addCode(codes, "{", indent);
        // new obj
        CodeJavaGenerator.addCode(codes, CodeJavaGenerator.setInstanceObject(typeName, varName), indent+1);
        for (NamedTypedValue f : getValue()){
            if (f.accessibleSchema != null && f.accessibleSchema.setterMethodName != null){
                String fName = varName;
                boolean fdeclar = false;
                if (f instanceof ObjectParam || f instanceof MapParam || f instanceof CollectionParam || f instanceof DateParam || f instanceof  BigDecimalParam || f instanceof BigIntegerParam){
                     fName = varName+"_"+f.getName();
                     fdeclar = true;
                }
                codes.addAll(f.newInstanceWithJava(fdeclar, true, fName, indent+1));

                if (f instanceof ObjectParam || f instanceof MapParam || f instanceof CollectionParam || f instanceof DateParam || f instanceof  BigDecimalParam || f instanceof BigIntegerParam){
                    CodeJavaGenerator.addCode(codes, CodeJavaGenerator.methodInvocation(varName, f.accessibleSchema.setterMethodName, fName)+CodeJavaGenerator.appendLast(),indent+1);
                }
            }else {
                String fName = varName+"."+f.getName();
                codes.addAll(f.newInstanceWithJava(false, true, fName, indent+1));
            }
        }

        CodeJavaGenerator.addCode(codes, "}", indent);
        return codes;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
        List<String> codes = new ArrayList<>();
        if (getValue() == null){
            CodeJavaGenerator.addCode(codes, CodeJavaGenerator.junitAssertNull(responseVarName), indent);
            return codes;
        }
        for (NamedTypedValue f : getValue()){
            String fName = null;
            if (f.accessibleSchema == null || f.accessibleSchema.isAccessible)
                fName = responseVarName+"."+f.getName();
            else{
                if (f.accessibleSchema.getterMethodName == null){
                    String msg = "Error: Object("+getType().getFullTypeName()+") has private field "+f.getName()+", but there is no getter method";
                    SimpleLogger.uniqueWarn(msg);
                    CodeJavaGenerator.addComment(codes, msg, indent);
                }else{
                    fName = responseVarName+"."+f.accessibleSchema.getterMethodName+"()";
                }
            }
            if (fName != null)
                codes.addAll(f.newAssertionWithJava(indent, fName, maxAssertionForDataInCollection));
        }
        return codes;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }

}
