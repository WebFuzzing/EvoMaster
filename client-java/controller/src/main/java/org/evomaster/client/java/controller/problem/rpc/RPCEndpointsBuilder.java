package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.AuthInRequestDto;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
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
     * for auth info specified with annotations
     * 1) regarding same annotation, they should have consistent keys
     * 2) annotation should not be null
     * @param authenticationDtoList
     */
    public static void validateRPCAuthInRequest(List<AuthenticationDto> authenticationDtoList){
        if (authenticationDtoList == null || authenticationDtoList.isEmpty()) return;

        Map<String, List<AuthInRequestDto>> group = new HashMap<>();
        authenticationDtoList.stream().filter(s-> s.authInRequest !=null).forEach(s->{
            if (s.authInRequest.annotationOnEndpoint == null)
                throw new RuntimeException("annotationName must be specified for auth info at index "+ authenticationDtoList.indexOf(s));
            if (!group.containsKey(s.authInRequest.annotationOnEndpoint))
                group.put(s.authInRequest.annotationOnEndpoint, new ArrayList<>());
            group.get(s.authInRequest.annotationOnEndpoint).add(s.authInRequest);
        });
        group.values().forEach(g->{
            if (g.size() > 1){
                List<String> keys = g.get(0).values.stream().map(a-> a.fieldKey).collect(Collectors.toList());
                g.forEach(a->{
                    List<String> akeys = a.values.stream().map(k-> k.fieldKey).collect(Collectors.toList());
                    if (akeys.size() != keys.size() || !akeys.containsAll(keys)){
                        throw new RuntimeException("keys for same annotation "+a.annotationOnEndpoint +" must be specified with same keys");
                    }
                });
            }
        });
    }

    /**
     * @param interfaceName the name of interface
     * @param rpcType          is the type of RPC, e.g., gRPC, Thrift
     * @return an interface schema for evomaster to access
     */
    public static InterfaceSchema build(String interfaceName, RPCType rpcType, Object client,
                                        List<String> skipEndpointsByName, List<String> skipEndpointsByAnnotation,
                                        List<String> involveEndpointsByName, List<String> involveEndpointsByAnnotation,
                                        List<AuthenticationDto> authenticationDtoList) {
        List<EndpointSchema> endpoints = new ArrayList<>();
        List<String> skippedEndpoints = new ArrayList<>();
        Map<Integer, EndpointSchema> authEndpoints = new HashMap<>();
        try {
            Class<?> interfaze = Class.forName(interfaceName);
            InterfaceSchema schema = new InterfaceSchema(interfaceName, endpoints, getClientClass(client) , rpcType, skippedEndpoints);
            for (Method m : interfaze.getDeclaredMethods()) {
                if (filterMethod(m, skipEndpointsByName, skipEndpointsByAnnotation, involveEndpointsByName, involveEndpointsByAnnotation))
                    endpoints.add(build(schema, m, rpcType, authenticationDtoList));
                else {
                    skippedEndpoints.add(m.getName());
                }

                AuthenticationDto auth = getRelatedAuthEndpoint(authenticationDtoList, interfaceName, m);
                if (auth != null){
                    int index = authenticationDtoList.indexOf(auth);
                    /*
                        handle endpoint which is for auth setup
                     */
                    authEndpoints.put(index, build(schema, m, rpcType, null));
                }
            }
            return schema;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find the interface with the name (" + interfaceName + ") and the error message is " + e.getMessage());
        }
    }

    private static AuthenticationDto getRelatedAuthEndpoint(List<AuthenticationDto> authenticationDtos, String interfaceName, Method method){
        if (authenticationDtos == null) return null;
        return authenticationDtos.stream().filter(a-> a.jsonAuthEndpoint != null
                && a.jsonAuthEndpoint.endpointName.equals(method.getName())
                && a.jsonAuthEndpoint.interfaceName.equals(interfaceName)).findAny().orElseGet(null);
    }

    private static boolean filterMethod(Method endpoint,
                                        List<String> skipEndpointsByName, List<String> skipEndpointsByAnnotation,
                                        List<String> involveEndpointsByName, List<String> involveEndpointsByAnnotation){
        if (skipEndpointsByName != null && involveEndpointsByName != null)
            throw new IllegalArgumentException("Error: skipEndpointsByName and involveEndpointsByName should not be specified at same time.");
        if (skipEndpointsByAnnotation != null && involveEndpointsByAnnotation != null)
            throw new IllegalArgumentException("Error: skipEndpointsByAnnotation and involveEndpointsByAnnotation should not be specified at same time.");

        if (skipEndpointsByName != null || skipEndpointsByAnnotation != null)
            return !anyMatchByNameAndAnnotation(endpoint, skipEndpointsByName, skipEndpointsByAnnotation);

        if (involveEndpointsByName != null || involveEndpointsByAnnotation != null)
            return anyMatchByNameAndAnnotation(endpoint, involveEndpointsByName, involveEndpointsByAnnotation);

        return true;
    }

    private static boolean anyMatchByNameAndAnnotation(Method endpoint, List<String> names, List<String> annotations){
        boolean anyMatch = false;
        if (annotations != null){
            for (Annotation annotation : endpoint.getAnnotations()){
                anyMatch = anyMatch || annotations.contains(annotation.annotationType().getName());
            }
        }

        if (names != null)
            anyMatch = anyMatch || names.contains(endpoint.getName());

        return anyMatch;
    }


    private static String getClientClass(Object client){
        if (client == null) return null;
        String clazzType = client.getClass().getName();

        // handle com.sun.proxy
        if (!clazzType.startsWith("com.sun.proxy.")){
            return clazzType;
        }

        Class<?>[] clazz = client.getClass().getInterfaces();
        if (clazz.length == 0){
            SimpleLogger.error("Error: the client is not related to any interface");
            return null;
        }

        if (clazz.length > 1)
            SimpleLogger.error("ERROR: the client has more than one interfaces");

        return clazz[0].getName();

    }

    private static EndpointSchema build(InterfaceSchema schema, Method method, RPCType rpcType, List<AuthenticationDto> authenticationDtoList) {
        List<NamedTypedValue> requestParams = new ArrayList<>();

        List<AuthenticationDto> authAnnotationDtos = getRelatedAuth(authenticationDtoList, method);
        List<Integer> authKeys = null;
        if (authAnnotationDtos != null)
            authKeys = authAnnotationDtos.stream().map(s-> authenticationDtoList.indexOf(s)).collect(Collectors.toList());
        List<String> authFields = null;
        if (authAnnotationDtos!= null && !authAnnotationDtos.isEmpty()){
            Optional<AuthenticationDto> authInRequest = authAnnotationDtos.stream().filter(s-> s.authInRequest != null).findAny();
            if (authInRequest.isPresent())
                authFields = authInRequest.get().authInRequest.values.stream().map(s-> s.fieldKey).collect(Collectors.toList());
        }
        for (Parameter p : method.getParameters()) {
            requestParams.add(buildInputParameter(schema, p, rpcType, authFields));
        }
        NamedTypedValue response = null;
        if (!method.getReturnType().equals(Void.TYPE)) {
            response = build(schema, method.getReturnType(), method.getGenericReturnType(), "return", rpcType, new ArrayList<>(), null);
        }

        List<NamedTypedValue> exceptions = null;
        if (method.getExceptionTypes().length > 0){
            exceptions = new ArrayList<>();
            for (int i = 0; i < method.getExceptionTypes().length; i++){
                NamedTypedValue exception = build(schema, method.getExceptionTypes()[i],
                        method.getGenericExceptionTypes()[i], "exception_"+i, rpcType, new ArrayList<>(), null);
                exceptions.add(exception);
            }
        }

        return new EndpointSchema(method.getName(), schema.getName(), schema.getClientInfo(), requestParams, response, exceptions, authAnnotationDtos!= null && !authAnnotationDtos.isEmpty(), authKeys);
    }


    private static List<AuthenticationDto> getRelatedAuth(List<AuthenticationDto> authenticationDtoList, Method method){
        if (authenticationDtoList == null) return null;
        List<String> annotations = Arrays.stream(method.getAnnotations()).map(s-> s.annotationType().getName()).collect(Collectors.toList());
        return authenticationDtoList.stream().filter(s-> (s.jsonAuthEndpoint != null && s.jsonAuthEndpoint.annotationOnEndpoint != null && annotations.contains(s.jsonAuthEndpoint.annotationOnEndpoint)) ||
                (s.authInRequest != null && s.authInRequest.annotationOnEndpoint != null && annotations.contains(s.authInRequest.annotationOnEndpoint))).collect(Collectors.toList());
    }

    private static NamedTypedValue buildInputParameter(InterfaceSchema schema, Parameter parameter, RPCType type, List<String> fields) {
        String name = parameter.getName();
        Class<?> clazz = parameter.getType();
        List<String> depth = new ArrayList<>();
        NamedTypedValue namedTypedValue = build(schema, clazz, parameter.getParameterizedType(), name, type, depth, fields);

        for (Annotation annotation: parameter.getAnnotations()){
            handleConstraint(namedTypedValue, annotation);
        }
        return namedTypedValue;
    }

    private static NamedTypedValue build(InterfaceSchema schema, Class<?> clazz, Type genericType, String name, RPCType rpcType, List<String> depth, List<String> authFieldKeys) {
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

                NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth, authFieldKeys);
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
                NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth, authFieldKeys);
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
                NamedTypedValue keyTemplate = build(schema, keyTemplateClazz, keyType,"keyTemplate", rpcType, depth, authFieldKeys);
                keyTemplate.setNullable(false);

                Class<?> valueTemplateClazz = getTemplateClass(valueType);
                NamedTypedValue valueTemplate = build(schema, valueTemplateClazz, valueType,"valueTemplate", rpcType, depth, authFieldKeys);
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
                    Field thrift_metamap = null;
                    for(Field f: clazz.getDeclaredFields()){
                        if (doSkipReflection(f.getName()) || doSkipField(f, rpcType))
                            continue;
                        if (rpcType == RPCType.THRIFT && isMetaMap(f)){
                            thrift_metamap = f;
                            continue;
                        }
                        NamedTypedValue field = build(schema, f.getType(), f.getGenericType(),f.getName(), rpcType, depth, authFieldKeys);
                        for (Annotation annotation : f.getAnnotations()){
                            handleConstraint(field, annotation);
                        }
                        fields.add(field);
                    }

                    if (thrift_metamap!=null){
                        handleMetaMap(thrift_metamap, fields);
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

        if (authFieldKeys!=null && authFieldKeys.contains(namedValue.getName())){
            namedValue.setForAuth(true);
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
                return THRIFT_SKIP.contains(field.getType().getName()) || doSkipFieldByName(field.getName(), type) || doSkipSchemas(field);
            }
            default: return false;
        }
    }

    // old version of thrift for ind1 case study
    private static boolean doSkipSchemas(Field field){
        if (!field.getName().equals("schemes")) return false;

        return field.getType().isAssignableFrom(Map.class)
                && getTemplateClass(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]).isAssignableFrom(Class.class);
    }

    private static boolean doSkipFieldByName(String name, RPCType type){
        switch (type){
            case THRIFT: return name.equals("__isset_bitfield") || name.equals("__isset_bit_vector") || name.matches("^__(.+)_ISSET_ID$");
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

    private static void handleMetaMap(Field metaMap_field, List<NamedTypedValue> fields){
        Object metaMap = null;
        try {
            metaMap = metaMap_field.get(null);

            if (metaMap instanceof Map){
                for (Object f : ((Map)metaMap).values()){
                    Field fname = f.getClass().getDeclaredField("fieldName");
                    fname.setAccessible(true);
                    String name = (String) fname.get(f);
                    NamedTypedValue field = findFieldByName(name, fields);
                    if (field!=null){
                        Field frequiredType = f.getClass().getDeclaredField("requirementType");
                        frequiredType.setAccessible(true);
                        byte required = (byte)frequiredType.get(f);
                        if (required == 1)
                            field.setNullable(false);
                        // TODO for handling default
                    }else {
                        SimpleLogger.error("Error: fail to find field in list but exist in metaMap, and the field name is "+ name);
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            SimpleLogger.error("Error: fail to set isNull based on metaMap of Thrift struct "+e.getMessage());
        }
    }

    private static NamedTypedValue findFieldByName(String name, List<NamedTypedValue> fields){
        for (NamedTypedValue f: fields){
            if (f.getName().equals(name)) return f;
        }
        return null;
    }

    private static int getDepthLevel(Class clazz, List<String> depth){
        String tag = getObjectTypeNameWithFlag(clazz, clazz.getName());
        int start = Math.max(0, depth.lastIndexOf(tag));
        return depth.subList(start, depth.size()).stream().filter(s-> !s.equals(tag) && s.startsWith(OBJECT_FLAG)).collect(Collectors.toSet()).size();
    }
}
