package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Reads the Spring Data Neo4j mapping of a class, i.e. its {@code @Node} annotation and the annotations on
 * its fields, to learn which labels, properties and relationships the nodes of that entity have.
 * <p>
 * The annotations are looked up by name, so this class has no dependency on Spring Data.
 */
public class Neo4jEntityAnalyzer {

    private static final String PACKAGE = "org.springframework.data.neo4j.core.schema.";
    private static final String NODE = PACKAGE + "Node";
    private static final String ID = PACKAGE + "Id";
    private static final String GENERATED_VALUE = PACKAGE + "GeneratedValue";
    private static final String PROPERTY = PACKAGE + "Property";
    private static final String RELATIONSHIP = PACKAGE + "Relationship";
    private static final String RELATIONSHIP_PROPERTIES = PACKAGE + "RelationshipProperties";
    private static final String TARGET_NODE = PACKAGE + "TargetNode";
    private static final String TRANSIENT = "org.springframework.data.annotation.Transient";
    private static final String VERSION = "org.springframework.data.annotation.Version";

    /** Classes that must be loadable for a SUT to be using Spring Data Neo4j. */
    public static final List<String> SPRING_DATA_NEO4J_NAMES = Arrays.asList(NODE, RELATIONSHIP);

    /**
     * Default constructor
     */
    public Neo4jEntityAnalyzer() {}

    /**
     * @param klass a class of the SUT
     * @return the entity the class maps, or {@code null} if it is not annotated with {@code @Node}
     * @throws Exception if the annotations cannot be read
     */
    public static Neo4jEntity analyze(Class<?> klass) throws Exception {

        Annotation node = getAnnotationByName(klass.getAnnotations(), NODE);
        if (node == null) {
            return null;
        }

        List<Neo4jEntityProperty> properties = new ArrayList<>();
        List<Neo4jEntityRelationship> relationships = new ArrayList<>();

        for (Field f : allInstanceFields(klass)) {
            Annotation[] annotations = f.getAnnotations();
            if (getAnnotationByName(annotations, TRANSIENT) != null
                    || getAnnotationByName(annotations, VERSION) != null) {
                continue;
            }

            Annotation relationship = getAnnotationByName(annotations, RELATIONSHIP);
            if (relationship != null) {
                Neo4jEntityRelationship r = toRelationship(f, relationship);
                if (r != null) {
                    relationships.add(r);
                }
                continue;
            }

            String type = graphType(f.getType());
            if (type == null) {
                SimpleLogger.debug("Skipping Neo4j property of unsupported type " + f.getType().getName()
                        + ": " + klass.getName() + "." + f.getName());
                continue;
            }
            boolean id = getAnnotationByName(annotations, ID) != null;
            boolean generated = getAnnotationByName(annotations, GENERATED_VALUE) != null;
            properties.add(new Neo4jEntityProperty(propertyName(f, annotations), type, id, generated,
                    minValue(f.getType()), maxValue(f.getType()), enumConstants(f.getType())));
        }

        return new Neo4jEntity(klass.getName(), labels(klass, node), properties, relationships);
    }

    /**
     * The labels of the nodes of the class: the primary label first, then any extra one. Without an
     * explicit primary label, Spring Data uses the simple name of the class.
     */
    private static List<String> labels(Class<?> klass, Annotation node) throws Exception {
        List<String> labels = new ArrayList<>();
        String primary = (String) attribute(node, "primaryLabel");
        String[] value = (String[]) attribute(node, "value");
        String[] extra = (String[]) attribute(node, "labels");
        if (primary != null && !primary.isEmpty()) {
            labels.add(primary);
        }
        for (String[] group : Arrays.asList(value, extra)) {
            for (String l : group) {
                if (!l.isEmpty() && !labels.contains(l)) {
                    labels.add(l);
                }
            }
        }
        if (labels.isEmpty()) {
            labels.add(klass.getSimpleName());
        }
        return labels;
    }

    private static String propertyName(Field f, Annotation[] annotations) throws Exception {
        Annotation property = getAnnotationByName(annotations, PROPERTY);
        if (property != null) {
            for (String element : Arrays.asList("name", "value")) {
                String name = (String) attribute(property, element);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }
        return f.getName();
    }

    private static Neo4jEntityRelationship toRelationship(Field f, Annotation relationship) throws Exception {
        String type = null;
        for (String element : Arrays.asList("type", "value")) {
            String t = (String) attribute(relationship, element);
            if (t != null && !t.isEmpty()) {
                type = t;
                break;
            }
        }
        if (type == null) {
            return null;
        }
        Object direction = attribute(relationship, "direction");
        boolean outgoing = direction == null || "OUTGOING".equals(direction.toString());

        Class<?> target = elementClass(f);
        if (target == null) {
            return null;
        }
        if (getAnnotationByName(target.getAnnotations(), RELATIONSHIP_PROPERTIES) != null) {
            target = allInstanceFields(target).stream()
                    .filter(t -> getAnnotationByName(t.getAnnotations(), TARGET_NODE) != null)
                    .map(Field::getType)
                    .findFirst().orElse(null);
            if (target == null) {
                return null;
            }
        }
        return new Neo4jEntityRelationship(type, target.getName(), outgoing);
    }

    /**
     * @return the class of the field, or of its elements if it is a collection
     */
    private static Class<?> elementClass(Field f) {
        if (!Collection.class.isAssignableFrom(f.getType())) {
            return f.getType();
        }
        Type generic = f.getGenericType();
        if (generic instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) generic).getActualTypeArguments();
            if (arguments.length == 1 && arguments[0] instanceof Class) {
                return (Class<?>) arguments[0];
            }
        }
        return null;
    }

    /**
     * @return the Neo4j property type a Java type is stored as, or {@code null} for the types we do not
     * know how to insert (collections, dates, nested objects...)
     */
    static String graphType(Class<?> javaType) {
        if (javaType == String.class || javaType.isEnum()) {
            return "STRING";
        }
        if (javaType == Long.class || javaType == long.class || javaType == Integer.class || javaType == int.class
                || javaType == Short.class || javaType == short.class || javaType == Byte.class || javaType == byte.class) {
            return "INTEGER";
        }
        if (javaType == Double.class || javaType == double.class || javaType == Float.class || javaType == float.class) {
            return "FLOAT";
        }
        if (javaType == Boolean.class || javaType == boolean.class) {
            return "BOOLEAN";
        }
        return null;
    }

    /**
     * @return the smallest value of an integer Java type narrower than long, so that an inserted value
     * can be read back into the field; null when there is no such bound
     */
    static Long minValue(Class<?> javaType) {
        if (javaType == Integer.class || javaType == int.class) return (long) Integer.MIN_VALUE;
        if (javaType == Short.class || javaType == short.class) return (long) Short.MIN_VALUE;
        if (javaType == Byte.class || javaType == byte.class) return (long) Byte.MIN_VALUE;
        return null;
    }

    static Long maxValue(Class<?> javaType) {
        if (javaType == Integer.class || javaType == int.class) return (long) Integer.MAX_VALUE;
        if (javaType == Short.class || javaType == short.class) return (long) Short.MAX_VALUE;
        if (javaType == Byte.class || javaType == byte.class) return (long) Byte.MAX_VALUE;
        return null;
    }

    /**
     * @return the names of the constants of an enum type, which are the only strings the field accepts
     */
    static List<String> enumConstants(Class<?> javaType) {
        List<String> names = new ArrayList<>();
        if (javaType.isEnum()) {
            for (Object constant : javaType.getEnumConstants()) {
                names.add(((Enum<?>) constant).name());
            }
        }
        return names;
    }

    /** Instance fields of the class and of its superclasses, superclass ones first. */
    private static List<Field> allInstanceFields(Class<?> klass) {
        List<Field> fields = new ArrayList<>();
        if (klass.getSuperclass() != null && klass.getSuperclass() != Object.class) {
            fields.addAll(allInstanceFields(klass.getSuperclass()));
        }
        for (Field f : klass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                fields.add(f);
            }
        }
        return fields;
    }

    private static Object attribute(Annotation annotation, String element) throws Exception {
        return annotation.annotationType().getMethod(element).invoke(annotation);
    }

    private static Annotation getAnnotationByName(Annotation[] annotations, String name) {
        return Arrays.stream(annotations)
                .filter(a -> a.annotationType().getName().equals(name))
                .findFirst().orElse(null);
    }
}
