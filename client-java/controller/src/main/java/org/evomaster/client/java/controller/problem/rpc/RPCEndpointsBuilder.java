package org.evomaster.client.java.controller.problem.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.api.dto.JsonAuthRPCEndpointDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
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
import java.util.stream.IntStream;

/**
 * created by manzhang on 2021/11/4
 */
public class RPCEndpointsBuilder {

    private final static ObjectMapper objectMapper = new ObjectMapper();

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
     * validate CustomizedRequestValueDto, eg,
     * 1) for any CustomizedRequestValueDto, keyValuePairs and keyValues could not be specified or null at the same time
     * 2) for keyValuePairs, if annotationOnEndpoint or specificEndpointName or specificRequestTypeName are specified, they should have consistent keys
     * 3) keyValues with respect to any specific annotationOnEndpoint or specificEndpointName or specificRequestTypeName should be specified only one time
     * @param customizedRequestValueDtos are customized info to be checked
     */
    public static void validateCustomizedValueInRequests(List<CustomizedRequestValueDto> customizedRequestValueDtos){
        if (customizedRequestValueDtos == null || customizedRequestValueDtos.isEmpty()) return;

        customizedRequestValueDtos.forEach(s->{
            if (s.keyValues != null && s.combinedKeyValuePairs != null)
                throw new IllegalArgumentException("Driver Config Error: keyValues and keyValuePairs should not be specified at the same time");
            if (s.keyValues == null && s.combinedKeyValuePairs == null)
                throw new IllegalArgumentException("Driver Config Error: one of keyValues and keyValuePairs must be specified, could not be null at the same time");
        });

        validateKeyValuePairs(customizedRequestValueDtos);
        validateKeyValues(customizedRequestValueDtos);
    }


    private static void validateKeyValues(List<CustomizedRequestValueDto> customizedRequestValueDtos){
        List<String> handled = new ArrayList<>();
        customizedRequestValueDtos.stream().filter(s-> s.keyValues !=null).forEach(s->{
            if (s.keyValues.key == null)
                throw new IllegalArgumentException("Driver Config Error: key must be specified when customizing keyValues");
            if (s.keyValues.values.isEmpty()){
                throw new IllegalArgumentException("Driver Config Error: at least one values is needed for customizing keyValues with the key "+s.keyValues.key);
            }
            String key = "key:"+s.keyValues.key+""+getKeyForCustomizedRequestValueDto(s);
            if (handled.contains(key))
                throw new IllegalArgumentException("Driver Config Error: "+key+" should be specified only once");
            handled.add(key);
        });
    }


    private static void validateKeyValuePairs(List<CustomizedRequestValueDto> customizedRequestValueDtos){
        Map<String, List<CustomizedRequestValueDto>> group = new HashMap<>();
        customizedRequestValueDtos.stream().filter(s-> s.combinedKeyValuePairs !=null && !s.combinedKeyValuePairs.isEmpty()).forEach(s->{

            String key = getKeyForCustomizedRequestValueDto(s);
            if (key.length() != 0){
                if (!group.containsKey(key))
                    group.put(key, new ArrayList<>());
                group.get(key).add(s);
            }
        });

        group.forEach((key, g) -> {
            if (g.size() > 1) {
                List<String> keys = g.get(0).combinedKeyValuePairs.stream().map(a -> a.fieldKey).collect(Collectors.toList());
                g.forEach(a -> {
                    List<String> akeys = a.combinedKeyValuePairs.stream().map(k -> k.fieldKey).collect(Collectors.toList());
                    if (akeys.size() != keys.size() || !akeys.containsAll(keys)) {
                        throw new IllegalArgumentException("Driver Config Error: keys for same " + key + " must be specified with same keys");
                    }
                });
            }
        });
    }

    private static String getKeyForCustomizedRequestValueDto(CustomizedRequestValueDto s){
        String key = "";
        if (s.annotationOnEndpoint != null)
            key += " annotationOnEndpoint_"+s.annotationOnEndpoint;
        if (s.specificEndpointName != null)
            key += " specificEndpointName_"+s.specificEndpointName;
        if (s.specificRequestTypeName != null)
            key += " specificRequestTypeName_"+s.specificRequestTypeName;
        return key;
    }

    /**
     * @param interfaceName the name of interface
     * @param rpcType          is the type of RPC, e.g., gRPC, Thrift
     * @param client is the corresponding client to maniplute the interface
     * @param skipEndpointsByName specifies a list of names of endpoints to be skipped during testing
     * @param skipEndpointsByAnnotation specifies a list of annotations applied on endpoints that could be skipped during testing
     * @param involveEndpointsByName specifies a list of names of endpoints to be involved during testing
     * @param involveEndpointsByAnnotation specifies a list of annotations applied on endpoints that are involved during testing
     * @param authenticationDtoList specifies a list of authentication info
     * @param customizedRequestValueDtos specifies a list of candidate values in requests
     * @return an interface schema for evomaster to access
     */
    public static InterfaceSchema build(String interfaceName, RPCType rpcType, Object client,
                                        List<String> skipEndpointsByName, List<String> skipEndpointsByAnnotation,
                                        List<String> involveEndpointsByName, List<String> involveEndpointsByAnnotation,
                                        List<AuthenticationDto> authenticationDtoList,
                                        List<CustomizedRequestValueDto> customizedRequestValueDtos) {
        List<EndpointSchema> endpoints = new ArrayList<>();
        List<EndpointSchema> endpointsForAuth = new ArrayList<>();
        List<String> skippedEndpoints = new ArrayList<>();
        Map<Integer, EndpointSchema> authEndpoints = new HashMap<>();
        try {
            Class<?> interfaze = Class.forName(interfaceName);
            InterfaceSchema schema = new InterfaceSchema(interfaceName, endpoints, getClientClass(client) , rpcType, skippedEndpoints, authEndpoints, endpointsForAuth);

            for (Method m : interfaze.getDeclaredMethods()) {
                if (filterMethod(m, skipEndpointsByName, skipEndpointsByAnnotation, involveEndpointsByName, involveEndpointsByAnnotation))
                    endpoints.add(build(schema, m, rpcType, authenticationDtoList, customizedRequestValueDtos));
                else {
                    skippedEndpoints.add(m.getName());
                }

                List<AuthenticationDto> auths = getRelatedAuthEndpoint(authenticationDtoList, interfaceName, m);
                if (auths != null && !auths.isEmpty()){
                    // handle endpoint which is for auth setup
                    EndpointSchema authEndpoint = build(schema, m, rpcType, null, customizedRequestValueDtos);
                    endpointsForAuth.add(authEndpoint);
                    for (AuthenticationDto auth: auths){
                        EndpointSchema copy = authEndpoint.copyStructure();
                        if (auth.jsonAuthEndpoint == null){
                            throw new IllegalArgumentException("Driver Config Error: now we only support auth info specified with JsonAuthRPCEndpointDto");
                        }
                        int index = authenticationDtoList.indexOf(auth);

                        // set value based on specified info
                        if (copy.getRequestParams().size() != auth.jsonAuthEndpoint.jsonPayloads.size())
                            throw new IllegalArgumentException("Driver Config Error: mismatched size of jsonPayloads ("+auth.jsonAuthEndpoint.classNames.size()+") with real endpoint ("+authEndpoint.getRequestParams().size()+").");
                        setAuthEndpoint(copy, auth.jsonAuthEndpoint);
                        authEndpoints.put(index, copy);
                    }
                }
            }
            return schema;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find the interface with the name (" + interfaceName + ") and the error message is " + e.getMessage());
        }
    }

    private static void setAuthEndpoint(EndpointSchema authEndpoint, JsonAuthRPCEndpointDto jsonAuthEndpoint) throws ClassNotFoundException{

        if (jsonAuthEndpoint.classNames != null && jsonAuthEndpoint.classNames.size() != jsonAuthEndpoint.jsonPayloads.size())
            throw new IllegalArgumentException("Driver Config Error: to specify inputs for auth endpoint, classNames and jsonPayloads should have same size");


        for (int i = 0; i < authEndpoint.getRequestParams().size(); i++){
            NamedTypedValue inputParam = authEndpoint.getRequestParams().get(i);
            String jsonString = jsonAuthEndpoint.jsonPayloads.get(i);

            if (jsonAuthEndpoint.classNames == null){
                setNamedValueBasedOnJsonString(inputParam,jsonString, i);
            }else{
                Class<?> clazz = Class.forName(jsonAuthEndpoint.classNames.get(i));

                try {
                    Object value = objectMapper.readValue(jsonString, clazz);
                    inputParam.setValueBasedOnInstance(value);
                } catch (JsonProcessingException e) {
                    SimpleLogger.uniqueWarn("Driver Config Error: a jsonPayload at ("+i+") cannot be read as the object "+jsonAuthEndpoint.classNames.get(i));
                    setNamedValueBasedOnJsonString(inputParam,jsonString, i);
                }
            }

        }
    }

    private static void setNamedValueBasedOnJsonString(NamedTypedValue inputParam, String jsonString, int index){
        if (inputParam instanceof StringParam || inputParam instanceof PrimitiveOrWrapperParam || inputParam instanceof ByteBufferParam){
            setNamedValueBasedOnCandidates(inputParam, jsonString);
        } else if (inputParam instanceof ObjectParam){
            try {
                JsonNode node = objectMapper.readTree(jsonString);
                List<NamedTypedValue> fields = new ArrayList<>();
                for (NamedTypedValue f: ((ObjectParam) inputParam).getType().getFields()){
                    NamedTypedValue v = f.copyStructure();
                    if (node.has(v.getName())){
                        setNamedValueBasedOnCandidates(f, node.textValue());
                        fields.add(v);
                    }else {
                        SimpleLogger.uniqueWarn("Driver Config Error: cannot find field with the name "+v.getName()+" in the specified json");
                    }
                }
                inputParam.setValue(fields);
            } catch (JsonProcessingException ex) {
                SimpleLogger.uniqueWarn("Driver Config Error: a jsonPayload at ("+index+") cannot be read as a JSON object with error:" +ex.getMessage());
            }
        }
    }



    private static List<AuthenticationDto> getRelatedAuthEndpoint(List<AuthenticationDto> authenticationDtos, String interfaceName, Method method){
        if (authenticationDtos == null) return null;
        for (AuthenticationDto dto : authenticationDtos){
            if (dto.jsonAuthEndpoint == null || dto.jsonAuthEndpoint.endpointName == null || dto.jsonAuthEndpoint.interfaceName == null){
                SimpleLogger.uniqueWarn("Driver Config Error: To specify JsonAuthRPCEndpointDto, endpointName and interfaceName cannot be null");
            }
        }
        return authenticationDtos.stream().filter(a-> a.jsonAuthEndpoint != null
                && a.jsonAuthEndpoint.endpointName.equals(method.getName())
                && a.jsonAuthEndpoint.interfaceName.equals(interfaceName)).collect(Collectors.toList());
    }

    private static boolean filterMethod(Method endpoint,
                                        List<String> skipEndpointsByName, List<String> skipEndpointsByAnnotation,
                                        List<String> involveEndpointsByName, List<String> involveEndpointsByAnnotation){
        if (skipEndpointsByName != null && involveEndpointsByName != null)
            throw new IllegalArgumentException("Driver Config Error: skipEndpointsByName and involveEndpointsByName should not be specified at same time.");
        if (skipEndpointsByAnnotation != null && involveEndpointsByAnnotation != null)
            throw new IllegalArgumentException("Driver Config Error: skipEndpointsByAnnotation and involveEndpointsByAnnotation should not be specified at same time.");

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

    private static EndpointSchema build(InterfaceSchema schema, Method method, RPCType rpcType, List<AuthenticationDto> authenticationDtoList,
                                        List<CustomizedRequestValueDto> customizedRequestValueDtos) {
        List<NamedTypedValue> requestParams = new ArrayList<>();

        List<AuthenticationDto> authAnnotationDtos = getRelatedAuth(authenticationDtoList, method);
        List<Integer> authKeys = null;
        if (authAnnotationDtos != null)
            authKeys = authAnnotationDtos.stream().map(s-> authenticationDtoList.indexOf(s)).collect(Collectors.toList());

        Set<String> relatedCustomization = new HashSet<>();

        for (Parameter p : method.getParameters()) {
            requestParams.add(buildInputParameter(schema, p, rpcType, getRelatedCustomization(customizedRequestValueDtos, method), relatedCustomization));
        }



        NamedTypedValue response = null;
        if (!method.getReturnType().equals(Void.TYPE)) {
            response = build(schema, method.getReturnType(), method.getGenericReturnType(), "return", rpcType, new ArrayList<>(), null, null, null);
        }

        List<NamedTypedValue> exceptions = null;
        if (method.getExceptionTypes().length > 0){
            exceptions = new ArrayList<>();
            for (int i = 0; i < method.getExceptionTypes().length; i++){
                NamedTypedValue exception = build(schema, method.getExceptionTypes()[i],
                        method.getGenericExceptionTypes()[i], "exception_"+i, rpcType, new ArrayList<>(), null, null, null);
                exceptions.add(exception);
            }
        }

        return new EndpointSchema(method.getName(),
                schema.getName(), schema.getClientInfo(), requestParams, response, exceptions,
                authAnnotationDtos!= null && !authAnnotationDtos.isEmpty(), authKeys, relatedCustomization);
    }


    private static List<AuthenticationDto> getRelatedAuth(List<AuthenticationDto> authenticationDtoList, Method method){
        if (authenticationDtoList == null) return null;
        List<String> annotations = Arrays.stream(method.getAnnotations()).map(s-> s.annotationType().getName()).collect(Collectors.toList());
        return authenticationDtoList.stream().filter(s-> (s.jsonAuthEndpoint != null &&
                (s.jsonAuthEndpoint.annotationOnEndpoint != null && annotations.contains(s.jsonAuthEndpoint.annotationOnEndpoint) )
        )).collect(Collectors.toList());
    }

    private static Map<Integer, CustomizedRequestValueDto> getRelatedCustomization(List<CustomizedRequestValueDto> customizedRequestValueDtos, Method method){
        if (customizedRequestValueDtos == null) return null;
        List<String> annotations = Arrays.stream(method.getAnnotations()).map(s-> s.annotationType().getName()).collect(Collectors.toList());
        List<CustomizedRequestValueDto> list = customizedRequestValueDtos.stream().filter(
                s-> (s.annotationOnEndpoint == null || annotations.contains(s.annotationOnEndpoint)) &&
                        (s.specificEndpointName == null || s.specificEndpointName.contains(method.getName()))
        ).collect(Collectors.toList());
        if (list.isEmpty()) return null;
        Map<Integer, CustomizedRequestValueDto> map = new HashMap<>();
        list.forEach(s->map.put(customizedRequestValueDtos.indexOf(s), s));
        return map;
    }

    private static NamedTypedValue buildInputParameter(InterfaceSchema schema, Parameter parameter, RPCType type,
                                                       Map<Integer,CustomizedRequestValueDto> customizationDtos, Set<String> relatedCustomization) {
        String name = parameter.getName();
        Class<?> clazz = parameter.getType();
        List<String> depth = new ArrayList<>();
        NamedTypedValue namedTypedValue = build(schema, clazz, parameter.getParameterizedType(), name, type, depth, customizationDtos, relatedCustomization, null);

        for (Annotation annotation: parameter.getAnnotations()){
            handleConstraint(namedTypedValue, annotation);
        }
        return namedTypedValue;
    }

    private static NamedTypedValue build(InterfaceSchema schema, Class<?> clazz, Type genericType, String name, RPCType rpcType, List<String> depth,
                                         Map<Integer, CustomizedRequestValueDto> customizationDtos, Set<String> relatedCustomization, AccessibleSchema accessibleSchema) {
        depth.add(getObjectTypeNameWithFlag(clazz, clazz.getName()));
        NamedTypedValue namedValue = null;

        try{

            if (PrimitiveOrWrapperType.isPrimitiveOrTypes(clazz)) {
                namedValue = PrimitiveOrWrapperParam.build(name, clazz, accessibleSchema);
            } else if (clazz == String.class) {
                namedValue = new StringParam(name, accessibleSchema);
            } else if (clazz.isEnum()) {
                String [] items = Arrays.stream(clazz.getEnumConstants()).map(Object::toString).toArray(String[]::new);
                EnumType enumType = new EnumType(clazz.getSimpleName(), clazz.getName(), items, clazz);
                EnumParam param = new EnumParam(name, enumType, accessibleSchema);
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

                NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth, customizationDtos, relatedCustomization, null);
                template.setNullable(false);
                CollectionType ctype = new CollectionType(clazz.getSimpleName(),clazz.getName(), template, clazz);
                ctype.depth = getDepthLevel(clazz, depth);
                namedValue = new ArrayParam(name, ctype, accessibleSchema);

            } else if (clazz == ByteBuffer.class){
                // handle binary of thrift
                namedValue = new ByteBufferParam(name, accessibleSchema);
            } else if (clazz.isAssignableFrom(List.class) || clazz.isAssignableFrom(Set.class)){
                if (genericType == null)
                    throw new RuntimeException("genericType should not be null for List and Set class");
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                Class<?> templateClazz = getTemplateClass(type);
                NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth, customizationDtos, relatedCustomization, null);
                template.setNullable(false);
                CollectionType ctype = new CollectionType(clazz.getSimpleName(),clazz.getName(), template, clazz);
                ctype.depth = getDepthLevel(clazz, depth);
                if (clazz.isAssignableFrom(List.class))
                    namedValue = new ListParam(name, ctype, accessibleSchema);
                else
                    namedValue = new SetParam(name, ctype, accessibleSchema);
            } else if (clazz.isAssignableFrom(Map.class)){
                if (genericType == null)
                    throw new RuntimeException("genericType should not be null for List and Set class");
                Type keyType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[1];

                Class<?> keyTemplateClazz = getTemplateClass(keyType);
                NamedTypedValue keyTemplate = build(schema, keyTemplateClazz, keyType,"keyTemplate", rpcType, depth, customizationDtos, relatedCustomization, null);
                keyTemplate.setNullable(false);

                Class<?> valueTemplateClazz = getTemplateClass(valueType);
                NamedTypedValue valueTemplate = build(schema, valueTemplateClazz, valueType,"valueTemplate", rpcType, depth, customizationDtos, relatedCustomization, null);
                MapType mtype = new MapType(clazz.getSimpleName(), clazz.getName(), new PairParam(new PairType(keyTemplate, valueTemplate), null), clazz);
                mtype.depth = getDepthLevel(clazz, depth);
                namedValue = new MapParam(name, mtype, accessibleSchema);
            } else if (clazz.isAssignableFrom(Date.class)){
                if (clazz == Date.class)
                    namedValue = new DateParam(name, accessibleSchema);
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
                        AccessibleSchema faccessSchema = null;
                        //check accessible
                        if (Modifier.isPublic(f.getModifiers())){
                            faccessSchema = new AccessibleSchema();
                        } else{
                            // find getter and setter
                            faccessSchema = new AccessibleSchema(false, findGetterOrSetter(clazz, f, false), findGetterOrSetter(clazz, f, true));
                        }
                        NamedTypedValue field = build(schema, f.getType(), f.getGenericType(),f.getName(), rpcType, depth, customizationDtos, relatedCustomization, faccessSchema);
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
                    ObjectParam oparam = new ObjectParam(name, otype, accessibleSchema);
                    schema.registerType(otype.copy(), oparam);
                    namedValue = oparam;
                }else {
                    CycleObjectType otype = new CycleObjectType(clazz.getSimpleName(), clazz.getName(), clazz);
                    otype.depth = getDepthLevel(clazz, depth);
                    ObjectParam oparam = new ObjectParam(name, otype, accessibleSchema);
                    schema.registerType(otype.copy(), oparam);
                    namedValue = oparam;
                }
            }
        }catch (ClassCastException e){
            throw new RuntimeException(String.format("fail to perform reflection on param/field: %s; class: %s; genericType: %s; class of genericType: %s; depth: %s; error info:%s",
                    name, clazz.getName(), genericType==null?"null":genericType.getTypeName(), genericType==null?"null":genericType.getClass().getName(), String.join(",", depth), e.getMessage()));
        }

        if (customizationDtos!=null){
            handleNamedValueWithCustomizedDto(namedValue, customizationDtos, relatedCustomization);
        }

        return namedValue;
    }

    private static String findGetterOrSetter(Class<?> clazz, Field field, boolean findGetter){
        List<Method> found;
        if (findGetter){
            found = Arrays.stream(clazz.getMethods()).filter(m->
                    Modifier.isPublic(m.getModifiers()) &&
                            m.getName().equalsIgnoreCase("get"+field.getName()) &&
                            m.getParameterCount() == 0
            ).collect(Collectors.toList());
        }else {
            found = Arrays.stream(clazz.getMethods()).filter(m->
                    Modifier.isPublic(m.getModifiers()) &&
                            m.getName().equalsIgnoreCase("set"+field.getName()) &&
                            m.getParameterCount() == 1 &&
                            m.getParameterTypes()[0].equals(field.getType())
            ).collect(Collectors.toList());
        }
        if (found.size() == 1)
            return found.get(0).getName();

        String msg = "RPC extract schema Error: cannot access field property, there exist "+found.size()+" methods to access the field "+ field.getName() + " for the class "+ clazz.getName();
        if (found.isEmpty()){
            SimpleLogger.uniqueWarn(msg);
            return null;
        }

        throw new IllegalStateException(msg);
    }

    private static void handleNamedValueWithCustomizedDto(NamedTypedValue namedTypedValue, Map<Integer, CustomizedRequestValueDto> customizationDtos, Set<String> relatedCustomization){

        List<String> candidateReferences = new ArrayList<>();
        List<NamedTypedValue> candidates = new ArrayList<>();
        customizationDtos.forEach((i, dto)->{
            if (dto.combinedKeyValuePairs != null && (dto.specificRequestTypeName == null || dto.specificRequestTypeName.equals(namedTypedValue.getType().getFullTypeName()))){
                dto.combinedKeyValuePairs.forEach(p->{
                    if (p.fieldKey.equals(namedTypedValue.getName())){
                        NamedTypedValue copy = namedTypedValue.copyStructure();
                        boolean ok = setNamedValueBasedOnCandidates(copy, p.fieldValue);
                        if (ok){
                            if (!candidateReferences.contains(""+i)){
                                relatedCustomization.add(""+i);
                                candidateReferences.add(""+i);
                                candidates.add(copy);
                            } else
                                throw new IllegalArgumentException("Error: there should not exist same key with the name "+p.fieldKey+"in a combinedKeyValuePairs");
                        }
                    }
                });
            }
        });

        if (!candidates.isEmpty()){
            namedTypedValue.setCandidateReferences(candidateReferences);
            namedTypedValue.setCandidates(candidates);
            return;
        }

        // check for keyValues
        List<CustomizedRequestValueDto> ikey = customizationDtos.values().stream().filter(s-> s.keyValues!= null && s.keyValues.key.equals(namedTypedValue.getName()) &&
                (s.specificRequestTypeName== null || s.specificRequestTypeName.equals(namedTypedValue.getType().getFullTypeName()))).collect(Collectors.toList());
        if (ikey.size() == 1){
            setCandidatesForNamedValue(namedTypedValue, ikey.get(0));
        } else if (ikey.size() > 1){
            throw new IllegalStateException("Error: more than one Dto for independent key with "+getKeyForCustomizedRequestValueDto(ikey.get(0)));
        }
    }

    private static void setCandidatesForNamedValue(NamedTypedValue namedTypedValue, CustomizedRequestValueDto customizedRequestValueDto){
        boolean handled = true;
        List<NamedTypedValue> candidates = new ArrayList<>();
        if (namedTypedValue instanceof PrimitiveOrWrapperParam || namedTypedValue instanceof StringParam || namedTypedValue instanceof ByteBufferParam){

            for (String v: customizedRequestValueDto.keyValues.values){
                NamedTypedValue copy= namedTypedValue.copyStructure();
                handled = handled && setNamedValueBasedOnCandidates(copy, v);
                candidates.add(copy);
            }
        }else {
            SimpleLogger.uniqueWarn("Error: Do not support configuring pre-defined values for the type "+namedTypedValue.getType().getFullTypeName());
            return;
        }

        if (handled){
            namedTypedValue.setCandidates(candidates);
        }
    }

    private static boolean setNamedValueBasedOnCandidates(NamedTypedValue copy, String value){
        try {
            if (copy instanceof PrimitiveOrWrapperParam){
                ((PrimitiveOrWrapperParam) copy).setValueBasedOnStringValue(value);
            }else if (copy instanceof StringParam)
                copy.setValue(value);
            else if (copy instanceof ByteBufferParam)
                copy.setValue(value.getBytes());


        }catch (RuntimeException exception){
            SimpleLogger.uniqueWarn("Error: fail to generate candidates with string value "+value+" for "+copy.getName() +" with type "+copy.getType().getFullTypeName());
            return false;
        }
        return true;
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
                        SimpleLogger.uniqueWarn("Error: fail to find field in list but exist in metaMap, and the field name is "+ name);
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            SimpleLogger.uniqueWarn("Error: fail to set isNull based on metaMap of Thrift struct "+e.getMessage());
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
