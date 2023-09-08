package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.*;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * object param
 */
public class ObjectParam extends NamedTypedValue<ObjectType, List<NamedTypedValue>> {

    public static final String PROTO3_BUILDER_METHOD = "newBuilder";

    public static final String PROTO3_OBJECT_BUILD_METHOD = "build";

    public ObjectParam(String name, ObjectType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        String clazzName = getType().getFullTypeName();
        Class<?> clazz = Class.forName(clazzName);
        try {
            Object instance = null;

            if (getType().spec == JavaDtoSpec.PROTO3){
                Object instanceBuilder = null;

                Method builderMethod = clazz.getMethod(PROTO3_BUILDER_METHOD);
                instanceBuilder = builderMethod.invoke(null);
                Class<?>  builderClazz = instanceBuilder.getClass();
                for (NamedTypedValue v: getValue()){
                    Class<?> setterInputClazz = v.getType().getClazz();
                    if (v.accessibleSchema.setterInputParams != null && v.accessibleSchema.setterInputParams.length > 0){
                        setterInputClazz = v.accessibleSchema.setterInputParams[0];
                    }

                    Method builderSetter = builderClazz.getMethod(v.accessibleSchema.setterMethodName,setterInputClazz);
                    builderSetter.invoke(instanceBuilder, v.newInstance());
                }
                Method buildMethod = builderClazz.getMethod(PROTO3_OBJECT_BUILD_METHOD);
                instance = buildMethod.invoke(instanceBuilder);

            } else if (getType().spec == JavaDtoSpec.DEFAULT){
                instance = clazz.newInstance();

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
        } catch (InvocationTargetException e) {
            throw new RuntimeException("fail to find the builder:"+clazzName+" with error msg:"+e.getMessage());
        }
    }

    private Method getSetter(Class<?> clazz, String setterName, TypeSchema type, Class<?> typeClass, int attemptTimes) throws NoSuchMethodException {

        try {
            Method m = clazz.getMethod(setterName, typeClass);
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
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        String typeName = getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        String varName = variableName;

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);
        addCode(codes, oneLineInstance(isDeclaration, doesIncludeName, typeName, varName, null,isJava, isNullable()), indent);
        if (isNull) return codes;

        addCode(codes, codeBlockStart(isJava), indent);

        String ownVarName = null;
        if (getType().spec == JavaDtoSpec.DEFAULT){
            // new obj
            addCode(codes, setInstanceObject(typeName, varName, isJava), indent + 1 );
            ownVarName = varName;
        }else{
            String varBuilderName = varName+"builder";
            addCode(codes, newBuilderProto3(typeName, varBuilderName, isJava), indent + 1);
            ownVarName = varBuilderName;
        }

        for (NamedTypedValue f : getValue()){
            if (f.accessibleSchema != null && f.accessibleSchema.setterMethodName != null){
                String fName = ownVarName;
                boolean fdeclar = false;
                if (needRenameField(f)){
                     fName = varName+"_"+f.getName();
                     fdeclar = true;
                }
                codes.addAll(f.newInstanceWithJavaOrKotlin(fdeclar, true, fName, indent+1, isJava, isNullable()));

                if (needRenameField(f)){
                    addCode(codes, methodInvocation(ownVarName, f.accessibleSchema.setterMethodName, fName, isJava, isNullable(), false)+ getStatementLast(isJava),indent+1);
                }
            }else {
                codes.addAll(f.newInstanceWithJavaOrKotlin(false, true,
                    fieldAccess(varName, f.getName(),isJava, isNullable(),false), indent+1, isJava, isNullable()));
            }
        }

        if (getType().spec == JavaDtoSpec.PROTO3){

            addCode(codes,
                setInstance(true, varName, methodInvocation(ownVarName, PROTO3_OBJECT_BUILD_METHOD, "",isJava, isNullable(), false), isJava),indent+1);
        }
        addCode(codes, codeBlockEnd(isJava), indent);
        return codes;
    }

    private boolean needRenameField(NamedTypedValue f){
        return f instanceof ObjectParam || f instanceof MapParam || f instanceof CollectionParam || f instanceof DateParam || f instanceof  BigDecimalParam || f instanceof BigIntegerParam || f instanceof Protobuf3ByteStringParam;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        List<String> codes = new ArrayList<>();
        if (getValue() == null){
            addCode(codes, junitAssertNull(responseVarName, isJava), indent);
            return codes;
        }
        for (NamedTypedValue f : getValue()){
            String fName = null;
            if (f.accessibleSchema == null || f.accessibleSchema.isAccessible){
                fName = fieldAccess(responseVarName, f.getName(),isJava, isNullable(),true);
            } else{
                if (f.accessibleSchema.getterMethodName == null){
                    String msg = "Error: Object("+getType().getFullTypeName()+") has private field "+f.getName()+", but there is no getter method";
                    SimpleLogger.uniqueWarn(msg);
                    addComment(codes, msg, indent);
                }else{
                    fName = methodInvocation(responseVarName, f.accessibleSchema.getterMethodName, "", isJava, isNullable(), true);
                }
            }
            if (fName != null)
                codes.addAll(f.newAssertionWithJavaOrKotlin(indent, fName, maxAssertionForDataInCollection, isJava));
        }
        return codes;
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return null;
    }


    @Override
    public List<String> referenceTypes() {
        List<String> references = new ArrayList<>();
        for (NamedTypedValue ref : getType().getFields()){
            if (ref instanceof ObjectParam){
                references.add(ref.getType().getFullTypeName());
                List<String> genericTypes = ((ObjectType)ref.getType()).getGenericTypes();
                if (genericTypes != null && !genericTypes.isEmpty())
                    references.addAll(genericTypes);
            }
            List<String> refrefTypes = ref.referenceTypes();
            if (refrefTypes != null)
                references.addAll(refrefTypes);
        }
        if (references.isEmpty()) return null;
        return references;
    }

}
