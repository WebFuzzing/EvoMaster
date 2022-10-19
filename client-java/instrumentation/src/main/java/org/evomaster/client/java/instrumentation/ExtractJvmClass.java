package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

public class ExtractJvmClass {


    /**
     *
     * @param jvmDtoNames a list of full name of dtos to extract
     * @return a map of dtoname to its schema
     */
    public static Map<String, String> extractAsSchema(List<String> jvmDtoNames) {
        Map<String, String> schemas = new LinkedHashMap<>();
        for (String dtoName: jvmDtoNames){
            Class<?> clazz;
            try {
                if (schemas.containsKey(dtoName)){
                    SimpleLogger.uniqueWarn("duplicated dto name:"+dtoName);
                    continue;
                }
                clazz = Class.forName(dtoName);
                /*
                    the class to extract will be cached inside ClassToSchema.getOrDeriveSchema
                    need to check whether we need to cache such specified class
                 */
                List<Class<?>> nested = new ArrayList<>();
                String schema = ClassToSchema.getOrDeriveSchemaAndNestedClasses(clazz, nested);
                schemas.putIfAbsent(dtoName, schema);
                for (Class<?> s : nested){
                    schemas.putIfAbsent(s.getName(), ClassToSchema.getOrDeriveNonNestedSchema(s));
                }
            } catch (ClassNotFoundException e) {
                SimpleLogger.uniqueWarn("Fail to extract Jvm DTO as schema:"+e.getMessage());
            }
        }
        return schemas;
    }

}
