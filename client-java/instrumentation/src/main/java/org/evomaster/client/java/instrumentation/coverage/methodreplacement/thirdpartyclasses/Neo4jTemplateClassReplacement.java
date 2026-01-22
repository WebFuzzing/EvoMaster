package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.Neo4jNodeSchema;
import org.evomaster.client.java.instrumentation.Neo4jRelationshipSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class Neo4jTemplateClassReplacement extends Neo4JOperationClassReplacement {

    private static final Neo4jTemplateClassReplacement singleton = new Neo4jTemplateClassReplacement();

    // Spring Data Neo4j annotation class names
    private static final String SDN_NODE_ANNOTATION = "org.springframework.data.neo4j.core.schema.Node";
    private static final String SDN_RELATIONSHIP_ANNOTATION = "org.springframework.data.neo4j.core.schema.Relationship";
    private static final String SDN_RELATIONSHIP_PROPERTIES_ANNOTATION = "org.springframework.data.neo4j.core.schema.RelationshipProperties";
    private static final String SDN_TARGET_NODE_ANNOTATION = "org.springframework.data.neo4j.core.schema.TargetNode";

    // Annotation method names
    private static final String METHOD_VALUE = "value";
    private static final String METHOD_LABELS = "labels";
    private static final String METHOD_TYPE = "type";
    private static final String METHOD_DIRECTION = "direction";

    // Direction values for normalization
    private static final String DIRECTION_INCOMING = "INCOMING";
    private static final String DEFAULT_DIRECTION = "OUTGOING";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.neo4j.core.Neo4jTemplate";
    }

    // Replacement method IDs
    private static final String SAVE_ID = "save";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = SAVE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NEO4J)
    public static <T> T save(Object neo4jTemplate, T instance) {
        try {
            addNeo4jNodeType(instance.getClass());

            Method saveMethod = getOriginal(singleton, SAVE_ID, neo4jTemplate);
            Object result = saveMethod.invoke(neo4jTemplate, instance);
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String FIND_BY_ID_ID = "findById";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = FIND_BY_ID_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NEO4J,
            castTo = "java.util.Optional")
    public static <T> Optional<T> findById(Object neo4jTemplate, Object id, Class<T> domainType) {
        try {
            addNeo4jNodeType(domainType);

            Method findByIdMethod = getOriginal(singleton, FIND_BY_ID_ID, neo4jTemplate);
            Object result = findByIdMethod.invoke(neo4jTemplate, id, domainType);
            return (Optional<T>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String FIND_ALL_ID = "findAll";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = FIND_ALL_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NEO4J,
            castTo = "java.util.List")
    public static <T> List<T> findAll(Object neo4jTemplate, Class<T> domainType) {
        try {
            addNeo4jNodeType(domainType);

            Method findAllMethod = getOriginal(singleton, FIND_ALL_ID, neo4jTemplate);
            Object result = findAllMethod.invoke(neo4jTemplate, domainType);
            return (List<T>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String COUNT_ID = "count";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = COUNT_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NEO4J)
    public static <T> long count(Object neo4jTemplate, Class<T> domainType) {
        try {
            addNeo4jNodeType(domainType);

            Method countMethod = getOriginal(singleton, COUNT_ID, neo4jTemplate);
            Object result = countMethod.invoke(neo4jTemplate, domainType);
            return (long) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String DELETE_BY_ID_ID = "deleteById";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = DELETE_BY_ID_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NEO4J)
    public static <T> void deleteById(Object neo4jTemplate, Object id, Class<T> domainType) {
        try {
            addNeo4jNodeType(domainType);

            Method deleteByIdMethod = getOriginal(singleton, DELETE_BY_ID_ID, neo4jTemplate);
            deleteByIdMethod.invoke(neo4jTemplate, id, domainType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static <T> void addNeo4jNodeType(Class<T> domainType) {
        String nodeLabel = extractNodeLabel(domainType);
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(domainType, true, Collections.emptyList());
        ExecutionTracer.addNeo4jNodeSchema(new Neo4jNodeSchema(nodeLabel, schema));

        // Extract relationships from the entity
        extractRelationships(domainType, nodeLabel);
    }

    private static <T> String extractNodeLabel(Class<T> domainType) {
        try {
            Class<?> nodeAnnotation = Class.forName(SDN_NODE_ANNOTATION);
            if (domainType.isAnnotationPresent((Class) nodeAnnotation)) {
                Object annotation = domainType.getAnnotation((Class) nodeAnnotation);
                Method valueMethod = nodeAnnotation.getMethod(METHOD_VALUE);
                String[] values = (String[]) valueMethod.invoke(annotation);
                if (values != null && values.length > 0 && !values[0].isEmpty()) {
                    return values[0];
                }
                Method labelsMethod = nodeAnnotation.getMethod(METHOD_LABELS);
                String[] labels = (String[]) labelsMethod.invoke(annotation);
                if (labels != null && labels.length > 0 && !labels[0].isEmpty()) {
                    return labels[0];
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Fall through to default
        }
        return domainType.getSimpleName();
    }

    private static <T> void extractRelationships(Class<T> domainType, String sourceNodeLabel) {
        try {
            Class<?> relationshipAnnotation = Class.forName(SDN_RELATIONSHIP_ANNOTATION);

            // Scan ALL fields including inherited ones
            List<Field> allFields = getAllFields(domainType);

            for (Field field : allFields) {
                if (field.isAnnotationPresent((Class<? extends Annotation>) relationshipAnnotation)) {
                    Annotation annotation = field.getAnnotation((Class<? extends Annotation>) relationshipAnnotation);

                    String relationshipType = extractRelationshipType(annotation, relationshipAnnotation, field);
                    String direction = extractRelationshipDirection(annotation, relationshipAnnotation);

                    Class<?> fieldTargetType = extractFieldTargetType(field);
                    Class<?> actualTargetNode = resolveActualTargetNode(fieldTargetType);
                    String targetNodeLabel = extractNodeLabel(actualTargetNode);

                    String propertiesSchema = extractRelationshipPropertiesSchema(fieldTargetType);

                    // Normalize: INCOMING means the current class is the target, so swap
                    String actualSource;
                    String actualTarget;
                    if (DIRECTION_INCOMING.equals(direction)) {
                        actualSource = targetNodeLabel;
                        actualTarget = sourceNodeLabel;
                    } else {
                        actualSource = sourceNodeLabel;
                        actualTarget = targetNodeLabel;
                    }

                    Neo4jRelationshipSchema relationshipSchema = new Neo4jRelationshipSchema(
                            relationshipType,
                            actualSource,
                            actualTarget,
                            propertiesSchema
                    );
                    ExecutionTracer.addNeo4jRelationshipSchema(relationshipSchema);
                }
            }
        } catch (ClassNotFoundException e) {
            // @Relationship annotation not available, skip relationship extraction
        }
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String extractRelationshipType(Annotation annotation, Class<?> relationshipAnnotation, Field field) {
        try {
            Method typeMethod = relationshipAnnotation.getMethod(METHOD_TYPE);
            String type = (String) typeMethod.invoke(annotation);
            if (type != null && !type.isEmpty()) {
                return type;
            }
            Method valueMethod = relationshipAnnotation.getMethod(METHOD_VALUE);
            String value = (String) valueMethod.invoke(annotation);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Fall through to default
        }
        return field.getName().toUpperCase();
    }

    private static String extractRelationshipDirection(Annotation annotation, Class<?> relationshipAnnotation) {
        try {
            Method directionMethod = relationshipAnnotation.getMethod(METHOD_DIRECTION);
            Object direction = directionMethod.invoke(annotation);
            return direction.toString();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return DEFAULT_DIRECTION;
        }
    }

    private static Class<?> extractFieldTargetType(Field field) {
        Class<?> fieldType = field.getType();

        // Handle arrays
        if (fieldType.isArray()) {
            return fieldType.getComponentType();
        }

        // Handle Collections (List, Set, etc.)
        if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0) {
                    return extractClassFromType(typeArgs[0]);
                }
            }
        }

        // Single reference (direct field type)
        return fieldType;
    }

    private static Class<?> extractClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        }
        return Object.class;
    }

    private static Class<?> resolveActualTargetNode(Class<?> fieldTargetType) {
        try {
            Class<?> relationshipPropertiesAnnotation = Class.forName(SDN_RELATIONSHIP_PROPERTIES_ANNOTATION);
            Class<?> targetNodeAnnotation = Class.forName(SDN_TARGET_NODE_ANNOTATION);

            // Check if fieldTargetType is a @RelationshipProperties class
            if (fieldTargetType.isAnnotationPresent((Class<? extends Annotation>) relationshipPropertiesAnnotation)) {
                // Find the field annotated with @TargetNode
                for (Field field : getAllFields(fieldTargetType)) {
                    if (field.isAnnotationPresent((Class<? extends Annotation>) targetNodeAnnotation)) {
                        return extractFieldTargetType(field);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // Annotations not available
        }
        // Not a @RelationshipProperties class, return as-is
        return fieldTargetType;
    }

    private static String extractRelationshipPropertiesSchema(Class<?> fieldTargetType) {
        try {
            Class<?> relationshipPropertiesAnnotation = Class.forName(SDN_RELATIONSHIP_PROPERTIES_ANNOTATION);

            // Check if target type has @RelationshipProperties annotation
            if (fieldTargetType.isAnnotationPresent((Class<? extends Annotation>) relationshipPropertiesAnnotation)) {
                return ClassToSchema.getOrDeriveSchemaWithItsRef(fieldTargetType, true, Collections.emptyList());
            }
        } catch (ClassNotFoundException e) {
            // @RelationshipProperties not available
        }
        return null;
    }
}
