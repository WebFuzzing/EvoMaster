package org.evomaster.client.java.instrumentation.object;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_REF_PATH;

/**
 * The SUT when getting data, it might unmarshal/deserialize it into a specif class.
 * Typical case is when a HTTP body payload is a JSON object, which should match a DTO
 * in the SUT.
 * So, we need a way to represent the schema of an object, independently from Java.
 * So, for simplicity, we use OpenAPI.
 * <br>
 * Note: the marshalling can depend on the actual used libraries, eg Jackson and GSON, as
 * they might use special annotations on the DTOs.
 */
public class ClassToSchema {

        /*
            https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#dataTypeFormat

        Common Name	    type	format	Comments
        integer	        integer	int32	signed 32 bits
        long	        integer	int64	signed 64 bits
        float	        number	float
        double	        number	double
        string	        string
        byte	        string	byte	base64 encoded characters
        binary	        string	binary	any sequence of octets
        boolean	        boolean
        date	        string	date	As defined by full-date - RFC3339
        dateTime	    string	date-time	As defined by date-time - RFC3339
        password	    string	password	Used to hint UIs the input needs to be obscured.

        We also provide extended support for the following types and formats (not defined in
        https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#dataTypeFormat)

        Common Name	        type	format	        Comments
        integer             integer int8            signed 8-bits
        integer             integer int16           signed 16-bits
        localDate           string  local-date      A date without a time-zone in the ISO-8601 calendar system
        localDateTime       string  local-date-time A date-time without a time-zone in the ISO-8601 calendar system
        localTime           string  local-time      A time without a time-zone in the ISO-8601 calendar system
         */

    /**
     * Key -> DTO class
     * Value -> its schema representation, as OpenAPI JSON object
     * WARNING: this is a mutable static state, but, being just a cache, should not hopefully
     * have any nasty negative side-effects.
     */
    private static final Map<Type, String> cacheSchema = new ConcurrentHashMap<>();

    /**
     * Key -> DTO class
     * Value -> the schema representations of the DTO class and its ref DTO class
     */
    private static final Map<Type, String> cacheSchemaWithItsRef = new ConcurrentHashMap<>();

    /**
     * Key -> DTO class
     * Value -> the schema representations of the DTO class and its ref DTO class
     */
    private static final Map<Type, Map<String, String>> cacheMapOfDtoAndItsRefToSchemas = new ConcurrentHashMap<>();


    private static final String fieldRefPrefix = "{\"$ref\":\"";

    private static final String fieldRefPostfix = "\"}";


    public static void registerSchemaIfNeeded(Class<?> valueType) {
        registerSchemaIfNeeded(valueType, false, Collections.emptyList());
    }

    public static void registerSchemaIfNeeded(Class<?> valueType, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {

        if (valueType == null) {
            return;
        }

        if (valueType.getName().startsWith("io.swagger.")) {
            //no point in dealing with this.
            //also it happens in E2E, where it leads to a infinite recursion
            return;
        }

        try {
            String name = valueType.getName();
            if (!UnitsInfoRecorder.isDtoSchemaRegister(name)) {
                List<Class<?>> embedded = new ArrayList<>();
                String schema = ClassToSchema.getOrDeriveSchema(valueType, embedded, objectFieldsRequired, converters);
                UnitsInfoRecorder.registerNewParsedDto(name, schema);
                ExecutionTracer.addParsedDtoName(name);
                if (!embedded.isEmpty()){
                    embedded.forEach(e -> registerSchemaIfNeeded(e, objectFieldsRequired, converters));
                }

            }
        } catch (Exception e) {
            SimpleLogger.warn("Fail to get schema for Class:" + valueType.getName(), e);
            /*
                fail with tests
             */
            //assert(false);
        }

    }
    /**
     *
     * @return a schema representation of the class along with schemas of its ref classes in the form
     *      "kclass name: { "kclass name": "schema", "ref class name": "schema", .... }"
     *
     * it is mainly used by [JsonTaint.handlePossibleJsonTaint]
     *
     * For example (see more details with ClassToSchemaTest.testCycleDto),
     * public class CycleDtoA {
     *     private String cycleAId;
     *     private CycleDtoB cycleDtoB;
     * }
     *
     * public class CycleDtoB {
     *     private String cycleBId;
     *     private CycleDtoA cycleDtoA;
     * }
     *
     * for CycleDtoA, it will return
     * "org.evomaster.client.java.instrumentation.object.dtos.CycleDtoA":{
     *      "org.evomaster.client.java.instrumentation.object.dtos.CycleDtoA":{"type":"object", "properties": {"cycleAId":{"type":"string"},"cycleDtoB":{"$ref":"#/components/schemas/org.evomaster.client.java.instrumentation.object.dtos.CycleDtoB"}}},
     *      "org.evomaster.client.java.instrumentation.object.dtos.CycleDtoB":{"type":"object", "properties": {"cycleBId":{"type":"string"},"cycleDtoA":{"$ref":"#/components/schemas/org.evomaster.client.java.instrumentation.object.dtos.CycleDtoA"}}}}
     *
     */

    public static String getOrDeriveSchemaWithItsRef(Class<?> klass) {
        return getOrDeriveSchemaWithItsRef(klass, false, Collections.emptyList());
    }

    public static String getOrDeriveSchemaWithItsRef(Class<?> klass, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {
        if (!cacheSchemaWithItsRef.containsKey(klass)) {
            StringBuilder sb = new StringBuilder();
            Map<String, String> map = getOrDeriveSchemaAndNestedClasses(klass, objectFieldsRequired, converters);
            sb.append("{");
            sb.append(map.get(klass.getName()));
            map.keySet().stream().filter(s -> !s.equals(klass.getName())).forEach(s ->
                    sb.append(",").append(map.get(s)));

            sb.append("}");
            cacheSchemaWithItsRef.put(klass, named(klass.getName(), sb.toString()));
        }

        return cacheSchemaWithItsRef.get(klass);
    }

    public static String getOrDeriveNonNestedSchema(Class<?> klass, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {
        return getOrDeriveSchema(klass, Collections.emptyList(), objectFieldsRequired, converters);
    }

    /**
     * @return a schema representation of the class in the form "name: {...}"
     */
    public static String getOrDeriveNonNestedSchema(Class<?> klass) {
        return getOrDeriveNonNestedSchema(klass, false, Collections.emptyList());
    }


    /**
     * @param nested is a list of nested classes
     * @return a schema representation of the class in the form "name: {...}", ie
     * like a field entry in an OpenAPI object definition
     */
    public static String getOrDeriveSchema(Class<?> klass, List<Class<?>> nested) {
        return getOrDeriveSchema(klass, nested, false, Collections.emptyList());
    }

    public static String getOrDeriveSchema(Class<?> klass, List<Class<?>> nested, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {
        if (!cacheSchema.containsKey(klass)) {
            cacheSchema.put(klass, getOrDeriveSchema(klass.getName(), klass, false, nested, objectFieldsRequired, converters));
        }

        return cacheSchema.get(klass);
    }

    private static String getOrDeriveSchema(String name, Type type, Boolean useRefObject, List<Class<?>> nested, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {

        // TODO might handle collection and map in the cache later
        if (cacheSchema.containsKey(type) && !useRefObject && !isCollectionOrMap(type)) {
            return cacheSchema.get(type);
        }


        String schema = getSchema(type, useRefObject, nested, false, objectFieldsRequired, converters);

        String namedSchema = named(name, schema);

        /*
            we put the complete schema into cacheSchema
         */
        if (!schema.startsWith(fieldRefPrefix) && !isCollectionOrMap(type))
            cacheSchema.put(type, namedSchema);

        return namedSchema;
    }

    private static boolean isCollectionOrMap(Type type) {
        if (!(type instanceof Class)) return false;
        Class<?> kclazz = (Class<?>) type;
        return kclazz.isArray() || Collection.class.isAssignableFrom(kclazz) || Map.class.isAssignableFrom(kclazz);
    }


    public static Map<String, String> getOrDeriveSchemaAndNestedClasses(Class<?> klass, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {
        if (!cacheMapOfDtoAndItsRefToSchemas.containsKey(klass)) {
            List<Class<?>> nested = new ArrayList<>();
            registerSchemaIfNeeded(klass, objectFieldsRequired, converters);
            findAllNestedClassAndRegisterThemIfNeeded(klass, nested, objectFieldsRequired, converters);
            Map<String, String> map = new LinkedHashMap<>();
            for (Class<?> nkclass : nested) {
                map.putIfAbsent(nkclass.getName(), getOrDeriveNonNestedSchema(nkclass, objectFieldsRequired, converters));
            }

            cacheMapOfDtoAndItsRefToSchemas.put(klass, map);
        }
        return cacheMapOfDtoAndItsRefToSchemas.get(klass);
    }

    private static void findAllNestedClassAndRegisterThemIfNeeded(Class<?> klass, List<Class<?>> nested, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {
        if (!nested.contains(klass)) {
            List<Class<?>> innerNested = new ArrayList<>();
            getSchema(klass, false, innerNested, true, objectFieldsRequired, converters);
            nested.add(klass);
            List<Class<?>> toAdd = innerNested.stream().filter(s -> !nested.contains(s)).collect(Collectors.toList());
            if (toAdd.isEmpty()) return;
            toAdd.forEach(a -> findAllNestedClassAndRegisterThemIfNeeded(a, nested, objectFieldsRequired, converters));
        }
    }

    private static String named(String name, String jsonObject) {
        return "\"" + name + "\":" + jsonObject;
    }


    /**
     * @param useRefObject represents whether to represent the object with ref
     * @param nested       is a list of nested classes
     * @param allNested    represents whether to add all nested into [nested]
     * @param objectFieldsRequired    represents whether set fields of objects as required
     */
    private static String getSchema(Type type, Boolean useRefObject, List<Class<?>> nested, boolean allNested, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {

        Class<?> klass = null;
        if (type instanceof Class) {
            klass = (Class<?>) type;
        }
        ParameterizedType pType = null;
        if (type instanceof ParameterizedType) {
            pType = (ParameterizedType) type;
        }

        if (klass != null) {
            if (klass.isEnum()) {
                String[] items = Arrays.stream(klass.getEnumConstants()).map(e -> getNameEnumConstant(e)).toArray(String[]::new);
                return fieldEnumSchema(items);
            }

            if (String.class.isAssignableFrom(klass)) {
                return fieldSchema("string");
            }

            if (Byte.class.isAssignableFrom(klass) || Byte.TYPE == klass) {
                return fieldSchema("integer", "int8");
            }
            if (Short.class.isAssignableFrom(klass) || Short.TYPE == klass) {
                return fieldSchema("integer", "int16");
            }
            if (Integer.class.isAssignableFrom(klass) || Integer.TYPE == klass) {
                return fieldSchema("integer", "int32");
            }
            if (Long.class.isAssignableFrom(klass) || Long.TYPE == klass) {
                return fieldSchema("integer", "int64");
            }
            if (Float.class.isAssignableFrom(klass) || Float.TYPE == klass) {
                return fieldSchema("number", "float");
            }
            if (Double.class.isAssignableFrom(klass) || Double.TYPE == klass) {
                return fieldSchema("number", "double");
            }
            if (Boolean.class.isAssignableFrom(klass) || Boolean.TYPE == klass) {
                return fieldSchema("boolean");
            }
            if (BigDecimal.class.isAssignableFrom(klass)) {
                //TODO not 100% sure this is correct...
                return fieldSchema("integer", "int64");
            }
            if (Date.class.isAssignableFrom(klass)) {
                return fieldSchema("string", "date");
            }
            if (LocalDate.class.isAssignableFrom(klass)) {
                return fieldSchema("string", "local-date");
            }
            if (LocalDateTime.class.isAssignableFrom(klass)) {
                return fieldSchema("string", "local-date-time");
            }
            if (LocalTime.class.isAssignableFrom(klass)) {
                return fieldSchema("string", "local-time");
            }
            for (CustomTypeToOasConverter converter : converters) {
                if (converter.isInstanceOf(klass)) {
                    return converter.convert();
                }
            }
        }

        if ((klass != null && (klass.isArray() || Collection.class.isAssignableFrom(klass)))
                ||
                (pType != null && (Collection.class.isAssignableFrom((Class) pType.getRawType()) ))) {
            return fieldArraySchema(klass, pType, nested, allNested, objectFieldsRequired, converters);
        }

        //TOOD Map
        if ((klass != null && Map.class.isAssignableFrom(klass)) || pType != null && Map.class.isAssignableFrom((Class) pType.getRawType())) {
            if (pType != null && pType.getActualTypeArguments().length > 0) {
                Type keyType = pType.getActualTypeArguments()[0];
                if (keyType != String.class) {
                    throw new IllegalStateException("only support Map with String key");
                }
            }

            return fieldStringKeyMapSchema(klass, pType, nested, allNested, objectFieldsRequired, converters);
        }

        if (useRefObject) {
            // register this class
            if ((allNested || !UnitsInfoRecorder.isDtoSchemaRegister(klass.getName())) && !nested.contains(klass)) {
                nested.add(klass);
            }
            return fieldObjectRefSchema(klass.getName());
        }


        List<String> properties = new ArrayList<>();
        List<String> propertiesNames = new ArrayList<>();

        //general object, let's look at its fields
        Class<?> target = klass;
        while (target != null) {
            for (Field f : target.getDeclaredFields()) {
                if (!shouldAddToSchema(f)) {
                    continue;
                }
                String fieldName = getName(f);
                String fieldSchema = null;
                if (allNested) {
                    fieldSchema = named(fieldName, getSchema(f.getGenericType(), true, nested, true, objectFieldsRequired, converters));
                } else
                    fieldSchema = getOrDeriveSchema(fieldName, f.getGenericType(), true, nested, objectFieldsRequired, converters);
                properties.add(fieldSchema);
                propertiesNames.add("\"" + fieldName + "\"");
            }
            target = target.getSuperclass();
        }

        return fieldObjectSchema(properties, propertiesNames, objectFieldsRequired);
    }

    private static boolean shouldAddToSchema(Field field) {

        if (Modifier.isStatic(field.getModifiers()) ||
                Modifier.isTransient(field.getModifiers())) {
            return false;
        }

        /*
            Check annotations. However, it is bit tricky:
            - annotations could be on setters/getters, instead of the field
            - GSON is quite complex, as customizable, see
              https://www.baeldung.com/gson-exclude-fields-serialization
              Jackson should be easier
              https://fasterxml.github.io/jackson-annotations/javadoc/2.7/com/fasterxml/jackson/annotation/JsonIgnore.html

            anyway, worst case we are just adding fields that are going to be ignored.
            So, for now, just a simple check on annotation names should be fine.
            TODO: check if worthy if to have a full 100% support for Jackson and GSON
         */
        for (Annotation a : field.getAnnotations()) {
            String name = a.annotationType().getSimpleName();
            if (name.equalsIgnoreCase("Ignore")
                    || name.equalsIgnoreCase("Ignored")
                    || name.equalsIgnoreCase("Exclude")
                    || name.equalsIgnoreCase("Excluded")
                    || name.equalsIgnoreCase("JsonIgnore")
                    || name.equalsIgnoreCase("Skip")
                    || name.equalsIgnoreCase("Transient")) {
                return false;
            }
        }

        return true;
    }

    private static String getName(Field field) {
        //TODO there are other ways to define names
        for (Annotation a : field.getAnnotations()) {
            String name = a.annotationType().getName();
            if (name.equals("com.fasterxml.jackson.annotation.JsonProperty")
                    || name.equals("com.google.gson.annotations.SerializedName")
                    || name.equals("org.springframework.data.mongodb.core.mapping.Field")) {
                try {
                    Method m = a.annotationType().getMethod("value");
                    String value = (String) m.invoke(a);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return field.getName();
    }

    private static String fieldArraySchema(Class<?> klass, ParameterizedType pType, List<Class<?>> embedded, boolean allEmbedded, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {

        String item;

        if (klass != null) {
            if (klass.isArray()) {
                item = getSchema(klass.getComponentType(), true, embedded, allEmbedded, objectFieldsRequired, converters);
            } else {
                /*
                    This would happen if we have non-generic List or Set?
                    What to do? I guess can just use String
                 */
                item = getSchema(String.class, true, embedded, allEmbedded, objectFieldsRequired, converters);
            }
        } else {
            //either List<> or Set<>
            Type generic = pType.getActualTypeArguments()[0];
            item = getSchema(generic, true, embedded, allEmbedded, objectFieldsRequired, converters);
        }

        return "{\"type\":\"array\", \"items\":" + item + "}";
    }

    private static String fieldStringKeyMapSchema(Class<?> klass, ParameterizedType pType, List<Class<?>> embedded, boolean allEmbedded, boolean objectFieldsRequired, List<CustomTypeToOasConverter> converters) {

        String value;

        if (klass != null) {
            value = getSchema(String.class, true, embedded, allEmbedded, objectFieldsRequired, converters);
        } else {
            Type generic = pType.getActualTypeArguments()[1];
            value = getSchema(generic, true, embedded, allEmbedded, objectFieldsRequired, converters);
        }

        return "{\"type\":\"object\", \"additionalProperties\":" + value + "}";
    }

    private static String fieldObjectSchema(List<String> properties, List<String> propertiesNames, boolean objectFieldsRequired) {
        String p = properties.stream().collect(Collectors.joining(","));
        String r = propertiesNames.stream().collect(Collectors.joining(","));
        if (objectFieldsRequired) {
            return "{\"type\":\"object\", \"properties\": {" + p + "}, \"required\": [" + r + "]}";
        }
        return "{\"type\":\"object\", \"properties\": {" + p + "}}";
    }

    private static String fieldObjectRefSchema(String name) {
        return fieldRefPrefix + OPENAPI_REF_PATH + name + fieldRefPostfix;
    }

    private static String fieldSchema(String type) {
        return "{\"type\":\"" + type + "\"}";
    }

    private static String fieldSchema(String type, String format) {
        return "{\"type\":\"" + type + "\", \"format\":\"" + format + "\"}";
    }

    private static String fieldEnumSchema(String[] items) {
        return "{\"type\":\"string\", \"enum\":[" + Arrays.stream(items).map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]}";
    }

    /*
        duplicated code from RPCEndpointsBuilder
     */
    private static String getNameEnumConstant(Object object) {
        try {
            Method name = object.getClass().getMethod("name");
            name.setAccessible(true);
            return (String) name.invoke(object);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            SimpleLogger.warn("Driver Error: fail to extract name for enum constant", e);
            return object.toString();
        }
    }
}
