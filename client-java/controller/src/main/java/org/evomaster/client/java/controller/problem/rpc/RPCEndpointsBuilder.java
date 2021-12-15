package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.InterfaceSchema;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/4
 */
public class RPCEndpointsBuilder {

    private final static String OBJECT_FLAG = "OBJECT";
    private static String getObjectTypeNameWithFlag(Class<?> clazz, String name) {
        if (isNotCustomizedObject(clazz)) return name;
        return OBJECT_FLAG + ":" + name;
    }

    private static boolean isNotCustomizedObject(Class<?> clazz){
        return PrimitiveOrWrapperType.isPrimitiveOrTypes(clazz) || clazz == String.class ||
                clazz == ByteBuffer.class || clazz.isEnum() || clazz.isArray() ||
                clazz.isAssignableFrom(List.class) || clazz.isAssignableFrom(Set.class);

    }

    /**
     * @param interfaceName the name of interface
     * @param rpcType          is the type of RPC, e.g., gRPC, Thrift
     * @return an interface schema for evomaster to access
     */
    public static InterfaceSchema build(String interfaceName, RPCType rpcType, Object client) {
        List<EndpointSchema> endpoints = new ArrayList<>();
        try {
            Class<?> interfaze = Class.forName(interfaceName);
            InterfaceSchema schema = new InterfaceSchema(interfaceName, endpoints, (client != null)?client.getClass().getName():null, rpcType);
            for (Method m : interfaze.getDeclaredMethods()) {
                endpoints.add(build(schema, m, rpcType));
            }
            return schema;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find the interface with the name (" + interfaceName + ") and the error message is " + e.getMessage());
        }
    }

    private static EndpointSchema build(InterfaceSchema schema, Method method, RPCType rpcType) {
        List<NamedTypedValue> requestParams = new ArrayList<>();

        for (Parameter p : method.getParameters()) {
            requestParams.add(buildInputParameter(schema, p, rpcType));
        }
        NamedTypedValue response = null;
        if (!method.getReturnType().equals(Void.TYPE)) {
            response = build(schema, method.getReturnType(), method.getGenericReturnType(), "return", rpcType, new ArrayList<>());
        }

        List<NamedTypedValue> exceptions = null;
        if (method.getExceptionTypes().length > 0){
            exceptions = new ArrayList<>();
            for (int i = 0; i < method.getExceptionTypes().length; i++){
                NamedTypedValue exception = build(schema, method.getExceptionTypes()[i],
                        method.getGenericExceptionTypes()[i], "exception_"+i, rpcType, new ArrayList<>());
                exceptions.add(exception);
            }
        }

        return new EndpointSchema(method.getName(), requestParams, response, exceptions);
    }

    private static NamedTypedValue buildInputParameter(InterfaceSchema schema, Parameter parameter, RPCType type) {
        String name = parameter.getName();
        Class<?> clazz = parameter.getType();
        List<String> depth = new ArrayList<>();
        NamedTypedValue namedTypedValue = build(schema, clazz, parameter.getParameterizedType(), name, type, depth);

        for (Annotation annotation: parameter.getAnnotations()){
            handleConstraint(namedTypedValue, annotation);
        }
        return namedTypedValue;
    }

    private static NamedTypedValue build(InterfaceSchema schema, Class<?> clazz, Type genericType, String name, RPCType rpcType, List<String> depth) {
        depth.add(getObjectTypeNameWithFlag(clazz, clazz.getName()));
        NamedTypedValue namedValue = null;

        try{

            if (PrimitiveOrWrapperType.isPrimitiveOrTypes(clazz)) {
                namedValue = PrimitiveOrWrapperParam.build(name, clazz);
            } else if (clazz == String.class) {
                namedValue = new StringParam(name);
            } else if (clazz.isEnum()) {
                String [] items = Arrays.stream(clazz.getEnumConstants()).map(Object::toString).toArray(String[]::new);
                EnumType enumType = new EnumType(clazz.getSimpleName(), clazz.getName(), items, clazz);
                EnumParam param = new EnumParam(name, enumType);
                //register this type in the schema
                schema.registerType(enumType.copy(), param.copyStructure());
                namedValue = param;
            } else if (clazz.isArray()){

                Type type = null;
                Class<?> templateClazz = null;
                if (genericType instanceof GenericArrayType){
                    type = ((GenericArrayType)genericType).getGenericComponentType();
                    templateClazz = getTemplateClass(type);
                }else {
                    templateClazz = clazz.getComponentType();
                }

                NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth);
                template.setNullable(false);
                CollectionType ctype = new CollectionType(clazz.getSimpleName(),clazz.getName(), template, clazz);
                ctype.depth = getDepthLevel(clazz, depth);
                namedValue = new ArrayParam(name, ctype);

            } else if (clazz == ByteBuffer.class){
                // handle binary of thrift
                namedValue = new ByteBufferParam(name);
            } else if (clazz.isAssignableFrom(List.class) || clazz.isAssignableFrom(Set.class)){
                if (genericType == null)
                    throw new RuntimeException("genericType should not be null for List and Set class");
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                Class<?> templateClazz = getTemplateClass(type);
                NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth);
                template.setNullable(false);
                CollectionType ctype = new CollectionType(clazz.getSimpleName(),clazz.getName(), template, clazz);
                ctype.depth = getDepthLevel(clazz, depth);
                if (clazz.isAssignableFrom(List.class))
                    namedValue = new ListParam(name, ctype);
                else
                    namedValue = new SetParam(name, ctype);
            } else if (clazz.isAssignableFrom(Map.class)){
                if (genericType == null)
                    throw new RuntimeException("genericType should not be null for List and Set class");
                Type keyType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[1];

                Class<?> keyTemplateClazz = getTemplateClass(keyType);
                NamedTypedValue keyTemplate = build(schema, keyTemplateClazz, keyType,"keyTemplate", rpcType, depth);
                keyTemplate.setNullable(false);

                Class<?> valueTemplateClazz = getTemplateClass(valueType);
                NamedTypedValue valueTemplate = build(schema, valueTemplateClazz, valueType,"valueTemplate", rpcType, depth);
                MapType mtype = new MapType(clazz.getSimpleName(), clazz.getName(), new PairParam(new PairType(keyTemplate, valueTemplate)), clazz);
                mtype.depth = getDepthLevel(clazz, depth);
                namedValue = new MapParam(name, mtype);
            } else if (clazz.isAssignableFrom(Date.class)){
                if (clazz == Date.class)
                    namedValue = new DateParam(name);
                else
                    throw new RuntimeException("NOT support "+clazz.getName()+" date type in java yet");
            }else {
                if (clazz.getName().startsWith("java")){
                    throw new RuntimeException("NOT handle "+clazz.getName()+" class in java yet");
                }

                long cycleSize = depth.stream().filter(s-> s.equals(getObjectTypeNameWithFlag(clazz, clazz.getName()))).count();

                if (cycleSize == 1){
                    List<NamedTypedValue> fields = new ArrayList<>();
                    for(Field f: clazz.getDeclaredFields()){
                        if (doSkipReflection(f.getName()) || doSkipField(f, rpcType))
                            continue;
                        NamedTypedValue field = build(schema, f.getType(), f.getGenericType(),f.getName(), rpcType, depth);
                        for (Annotation annotation : f.getAnnotations()){
                            handleConstraint(field, annotation);
                        }
                        fields.add(field);
                    }
                    ObjectType otype = new ObjectType(clazz.getSimpleName(), clazz.getName(), fields, clazz);
                    otype.depth = getDepthLevel(clazz, depth);
                    ObjectParam oparam = new ObjectParam(name, otype);
                    schema.registerType(otype.copy(), oparam);
                    namedValue = oparam;
                }else {
                    CycleObjectType otype = new CycleObjectType(clazz.getSimpleName(), clazz.getName(), clazz);
                    otype.depth = getDepthLevel(clazz, depth);
                    ObjectParam oparam = new ObjectParam(name, otype);
                    schema.registerType(otype.copy(), oparam);
                    namedValue = oparam;
                }
            }
        }catch (ClassCastException e){
            throw new RuntimeException(String.format("fail to perform reflection on param/field: %s; class: %s; genericType: %s; class of genericType: %s; depth: %s; error info:%s",
                    name, clazz.getName(), genericType==null?"null":genericType.getTypeName(), genericType==null?"null":genericType.getClass().getName(), String.join(",", depth), e.getMessage()));
        }


        return namedValue;
    }

    private static void handleConstraint(NamedTypedValue namedTypedValue, Annotation annotation){
        if (annotation.annotationType().getName().startsWith("javax.validation.constraints")){
            JavaXConstraintHandler.handleParam(namedTypedValue, annotation);
        } else {
            SimpleLogger.info("annotation with "+ annotation.annotationType().getName()+" is not handled");
        }
    }

    private static Class<?> getTemplateClass(Type type){
        if (type instanceof ParameterizedType){
            return  (Class<?>) ((ParameterizedType)type).getRawType();
        }else if (type instanceof Class)
            return  (Class<?>) type;
        throw new RuntimeException("unhanded type:"+ type);
    }

    /**
     * there might exist some additional info generated by eg instrumentation
     * then we need to skip reflection on such info with the specified name
     * @param name is a name of object to check
     * @return whether to skip the object
     */
    private static boolean doSkipReflection(String name){
        return name.equals("$jacocoData");
    }

    private final static List<String> THRIFT_SKIP = Arrays.asList(
            "org.apache.thrift.protocol.TStruct",
            "org.apache.thrift.protocol.TField",
            "org.apache.thrift.TFieldIdEnum",
            "org.apache.thrift.scheme.SchemeFactory"
    );

    private static boolean doSkipField(Field field, RPCType type){
        switch (type){
            case THRIFT: {
                return THRIFT_SKIP.contains(field.getType().getName()) || isMetaMap(field) || doSkipFieldByName(field.getName(), type);
            }
            default: return false;
        }

    }

    private static boolean doSkipFieldByName(String name, RPCType type){
        switch (type){
            case THRIFT: return name.equals("__isset_bitfield") || name.matches("^__(.+)_ISSET_ID$");
            default: return false;
        }
    }

    private static boolean isMetaMap(Field field){
        boolean result = field.getName().equals("metaDataMap")
                && field.getType().isAssignableFrom(Map.class);
        if (!result) return result;
        Type genericType = field.getGenericType();

        Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[1];

        return valueType.getTypeName().equals("org.apache.thrift.meta_data.FieldMetaData");

    }

    private static int getDepthLevel(Class clazz, List<String> depth){
        String tag = getObjectTypeNameWithFlag(clazz, clazz.getName());
        int start = Math.max(0, depth.lastIndexOf(tag));
        return depth.subList(start, depth.size()).stream().filter(s-> !s.equals(tag) && s.startsWith(OBJECT_FLAG)).collect(Collectors.toSet()).size();
    }
}
